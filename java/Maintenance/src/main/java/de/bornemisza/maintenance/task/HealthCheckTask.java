package de.bornemisza.maintenance.task;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    CouchUsersPool httpPool;

    @Inject
    HealthChecks healthChecks;

    private ITopic<ClusterEvent> clusterMaintenanceTopic;
    private static final Set<String> FAILING_HOSTS = new HashSet<>();

    public HealthCheckTask() {
    }

    // Constructor for Unit Tests
    public HealthCheckTask(CouchUsersPool httpPool, ITopic<ClusterEvent> topic, HealthChecks healthChecks) {
        this.httpPool = httpPool;
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
        Map<String, HttpConnection> connections = httpPool.getAllConnections();
        for (Map.Entry<String, HttpConnection> entry : connections.entrySet()) {
            String hostname = entry.getKey();
            if (healthChecks.isCouchDbReady(entry.getValue())) {
                if (FAILING_HOSTS.contains(hostname)) {
                    FAILING_HOSTS.remove(hostname);
                    ClusterEvent clusterEvent = new ClusterEvent(hostname, ClusterEventType.HOST_HEALTHY);
                    this.clusterMaintenanceTopic.publish(clusterEvent);
                }
                // Host still healthy, just keep it in rotation
            }
            else {
                if (! FAILING_HOSTS.contains(hostname)) {
                    FAILING_HOSTS.add(hostname);
                    ClusterEvent clusterEvent = new ClusterEvent(hostname, ClusterEventType.HOST_UNHEALTHY);
                    this.clusterMaintenanceTopic.publish(clusterEvent);
                }
                // Host still unhealthy, just keep it out of rotation
            }
        }
    }

}
