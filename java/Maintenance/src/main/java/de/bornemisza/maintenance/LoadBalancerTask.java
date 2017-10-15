package de.bornemisza.maintenance;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.naming.NamingException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.rest.da.HttpPool;

public class LoadBalancerTask {

    @Resource(name="http/Base")
    HttpPool pool;

    @Resource
    private TimerService timerService;

    private final HazelcastInstance hazelcast;
    private final IMap<String, Integer> dbServerUtilisation;

    public LoadBalancerTask(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
        this.dbServerUtilisation = hazelcast.getMap(Config.UTILISATION);
    }

    // Constructor for Unit Tests
    public LoadBalancerTask(HazelcastInstance hazelcast, IMap<String, Integer> dbServerUtilisation) {
        this.hazelcast = hazelcast;
        this.dbServerUtilisation = dbServerUtilisation;
    }

    public Timer createTimer(ScheduleExpression expression, TimerConfig timerConfig) {
        return timerService.createCalendarTimer(expression, timerConfig);
    }

    @Timeout
    public void performMaintenance() {
        Set<String> utilisedHostnames = this.dbServerUtilisation.keySet();
        try {
            String serviceName = pool.getServiceName();
            List<String> dnsHostnames = new DnsProvider(hazelcast).getHostnamesForService(serviceName);
            updateDbServerUtilisation(utilisedHostnames, dnsHostnames);
        }
        catch (NamingException ex) {
            Logger.getAnonymousLogger().warning("Problem reading SRV-Records: " + ex.toString());
        }

        logNewQueueState();
    }

    void updateDbServerUtilisation(Set<String> utilisedHostnames, List<String> dnsHostnames) {
        boolean newHostDetected = false;
        for (String dnsHostname : dnsHostnames) {
            if (! utilisedHostnames.contains(dnsHostname)) {
                newHostDetected = true;
                this.dbServerUtilisation.put(dnsHostname, 0);
            }
        }
        for (String hostname : utilisedHostnames) {
            if (dnsHostnames.contains(hostname) && newHostDetected) {
                // start all hosts on equal terms
                this.dbServerUtilisation.put(hostname, 0);
            }
            else {
                // a host providing the service has just disappeared
                this.dbServerUtilisation.remove(hostname);
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
