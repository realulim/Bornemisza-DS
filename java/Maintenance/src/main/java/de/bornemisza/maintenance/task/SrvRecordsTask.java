package de.bornemisza.maintenance.task;

import java.util.List;
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
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.ClusterEvent.ClusterEventType;

import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.maintenance.CouchUsersPool;

@Stateless
public class SrvRecordsTask {

    @Resource
    private TimerService timerService;

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    CouchUsersPool httpPool;

    @Inject
    HealthChecks healthChecks;

    private IMap<String, Integer> dbServerUtilisation;
    private ITopic<ClusterEvent> clusterMaintenanceTopic;
    private Set<String> candidates;

    public SrvRecordsTask() {
    }

    @PostConstruct
    public void init() {
        this.dbServerUtilisation = hazelcast.getMap(Config.UTILISATION);
        this.clusterMaintenanceTopic = hazelcast.getReliableTopic(Config.TOPIC_CLUSTER_MAINTENANCE);
        this.candidates = hazelcast.getSet(Config.CANDIDATES);

    }

    // Constructor for Unit Tests
    public SrvRecordsTask(HazelcastInstance hz, CouchUsersPool httpPool, HealthChecks healthChecks) {
        this.hazelcast = hz;
        this.httpPool = httpPool;
        this.healthChecks = healthChecks;
    }

    public Timer createTimer(ScheduleExpression expression, TimerConfig timerConfig) {
        return timerService.createCalendarTimer(expression, timerConfig);
    }

    @Timeout
    public void srvRecordsMaintenance() {
        Set<String> utilisedHostnames = this.dbServerUtilisation.keySet();
        String serviceName = httpPool.getServiceName();
        List<String> dnsHostnames = new DnsProvider(hazelcast).getHostnamesForService(serviceName);
        updateDbServers(utilisedHostnames, dnsHostnames);
        logNewQueueState();
    }

    void updateDbServers(Set<String> utilisedHostnames, List<String> dnsHostnames) {
        for (String hostname : dnsHostnames) {
            if (! utilisedHostnames.contains(hostname)) {
                // a host providing the service is available, but not in rotation
                candidates.add(hostname);
                Logger.getAnonymousLogger().info("Candidate appeared: " + hostname);
            }
        }
        for (String hostname : utilisedHostnames) {
            if (! dnsHostnames.contains(hostname)) {
                // a host providing the service has just disappeared
                Logger.getAnonymousLogger().info("Host disappeared: " + hostname);
                ClusterEvent clusterEvent = new ClusterEvent(hostname, ClusterEventType.HOST_DISAPPEARED);
                this.clusterMaintenanceTopic.publish(clusterEvent);
            }
        }
    }

    void logNewQueueState() {
        StringBuilder sb = new StringBuilder("DbServerQueue");
        for (String hostname : dbServerUtilisation.keySet()) {
            sb.append(" | ").append(hostname).append(":").append(dbServerUtilisation.get(hostname));
        }
        Logger.getAnonymousLogger().info(sb.toString());
    }

}
