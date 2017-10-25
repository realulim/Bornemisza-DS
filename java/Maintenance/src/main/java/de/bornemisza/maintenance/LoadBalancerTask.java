package de.bornemisza.maintenance;

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

import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.da.DnsProvider;

@Stateless
public class LoadBalancerTask {

    @Resource
    private TimerService timerService;

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    HttpBasePool pool;

    private IMap<String, Integer> dbServerUtilisation;

    public LoadBalancerTask() {
    }

    @PostConstruct
    public void init() {
        this.dbServerUtilisation = hazelcast.getMap(Config.UTILISATION);
    }

    // Constructor for Unit Tests
    public LoadBalancerTask(IMap<String, Integer> dbServerUtilisation) {
        this.dbServerUtilisation = dbServerUtilisation;
    }

    public Timer createTimer(ScheduleExpression expression, TimerConfig timerConfig) {
        return timerService.createCalendarTimer(expression, timerConfig);
    }

    @Timeout
    public void performMaintenance() {
        Set<String> utilisedHostnames = this.dbServerUtilisation.keySet();
        String serviceName = pool.getServiceName();
        List<String> dnsHostnames = new DnsProvider(hazelcast).getHostnamesForService(serviceName);
        updateDbServerUtilisation(utilisedHostnames, dnsHostnames);

        logNewQueueState();
    }

    void updateDbServerUtilisation(Set<String> utilisedHostnames, List<String> dnsHostnames) {
        for (String hostname : utilisedHostnames) {
            if (! dnsHostnames.contains(hostname)) {
                // a host providing the service has just disappeared
                this.dbServerUtilisation.remove(hostname);
                Logger.getAnonymousLogger().info("Host has disappeared: " + hostname);
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
