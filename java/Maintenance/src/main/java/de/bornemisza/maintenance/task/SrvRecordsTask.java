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
import de.bornemisza.rest.HttpConnection;

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

    public SrvRecordsTask() {
    }

    @PostConstruct
    public void init() {
        this.dbServerUtilisation = hazelcast.getMap(Config.UTILISATION);
        this.clusterMaintenanceTopic = hazelcast.getReliableTopic(Config.TOPIC_CLUSTER_MAINTENANCE);
    }

    // Constructor for Unit Tests
    public SrvRecordsTask(IMap<String, Integer> dbServerUtilisation, ITopic<ClusterEvent> topic, CouchUsersPool httpPool, HealthChecks healthChecks) {
        this.dbServerUtilisation = dbServerUtilisation;
        this.clusterMaintenanceTopic = topic;
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
                HttpConnection conn = httpPool.getAllConnections().get(hostname);
                if (conn == null || healthChecks.isCouchDbReady(conn)) {
                    // it's either new or healthy, so we can add it to the rotation
                    ClusterEvent clusterEvent = new ClusterEvent(hostname, ClusterEventType.HOST_APPEARED);
                    this.clusterMaintenanceTopic.publish(clusterEvent);
                }
            }
        }
        for (String hostname : utilisedHostnames) {
            if (! dnsHostnames.contains(hostname)) {
                // a host providing the service has just disappeared
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
