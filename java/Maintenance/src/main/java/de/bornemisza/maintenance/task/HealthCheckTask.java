package de.bornemisza.maintenance.task;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.ClusterEvent.ClusterEventType;
import de.bornemisza.loadbalancer.Config;
import de.bornemisza.maintenance.CouchUsersPool;
import de.bornemisza.rest.HttpConnection;

@Stateless
public class HealthCheckTask {

    @Resource
    private TimerService timerService;

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    CouchUsersPool usersPool;

    @Inject
    HealthChecks healthChecks;

    private ITopic<ClusterEvent> clusterMaintenanceTopic;
    private static final Set<String> FAILING_HOSTS = new HashSet<>();

    public HealthCheckTask() {
    }

    // Constructor for Unit Tests
    public HealthCheckTask(HazelcastInstance hz, CouchUsersPool httpPool, ITopic<ClusterEvent> topic, HealthChecks healthChecks) {
        this.hazelcast = hz;
        this.usersPool = httpPool;
        this.clusterMaintenanceTopic = topic;
        this.healthChecks = healthChecks;
    }

    @PostConstruct
    public void init() {
        this.clusterMaintenanceTopic = hazelcast.getReliableTopic(Config.TOPIC_CLUSTER_MAINTENANCE);
    }

    public Timer createTimer(ScheduleExpression expression, TimerConfig timerConfig) {
        return timerService.createCalendarTimer(expression, timerConfig);
    }

    @Timeout
    public void healthChecks() {
        Map<String, HttpConnection> connections = usersPool.getAllConnections();
        for (Map.Entry<String, HttpConnection> entry : connections.entrySet()) {
            String hostname = entry.getKey();
            if (healthChecks.isCouchDbReady(entry.getValue())) {
                if (FAILING_HOSTS.contains(hostname)) {
                    FAILING_HOSTS.remove(hostname);
                    Logger.getAnonymousLogger().info("Previously failing host " + hostname + " healthy again.");
                    ClusterEvent clusterEvent = new ClusterEvent(hostname, ClusterEventType.HOST_HEALTHY);
                    this.clusterMaintenanceTopic.publish(clusterEvent);
                }
                // Host still healthy, just keep it in rotation
            }
            else {
                if (! FAILING_HOSTS.contains(hostname)) {
                    FAILING_HOSTS.add(hostname);
                    Logger.getAnonymousLogger().info("Previously healthy host " + hostname + " failing.");
                    ClusterEvent clusterEvent = new ClusterEvent(hostname, ClusterEventType.HOST_UNHEALTHY);
                    this.clusterMaintenanceTopic.publish(clusterEvent);
                }
                // Host still unhealthy, just keep it out of rotation
            }
        }
        checkCandidates();
    }

    private void checkCandidates() {
        Set<String> candidates = hazelcast.getSet(Config.CANDIDATES);
        for (String candidate : candidates) {
            try {
                HttpConnection conn = usersPool.createConnection(candidate);
                if (healthChecks.isCouchDbReady(conn)) {
                    // Candidate is healthy and can be promoted into rotation
                    Logger.getAnonymousLogger().info("Candidate " + candidate + " healthy.");
                    ClusterEvent clusterEvent = new ClusterEvent(candidate, ClusterEventType.CANDIDATE_HEALTHY);
                    this.clusterMaintenanceTopic.publish(clusterEvent);
                }
            }
            catch (Exception e) {
                Logger.getAnonymousLogger().severe("Could not check candidates: " + e.toString());
            }
        }
    }

}
