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
import javax.naming.NamingException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.rest.da.HttpPool;

@Stateless
public class LoadBalancerTask {

    @Resource
    private TimerService timerService;

    @Inject
    HazelcastInstance hazelcast;

    @Resource(name="http/Base")
    HttpPool pool;

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
Logger.getAnonymousLogger().info("New Host detected: " + dnsHostname);
            }
        }
        for (String hostname : utilisedHostnames) {
Logger.getAnonymousLogger().info("Checking " + hostname);
            if (dnsHostnames.contains(hostname)) {
                if (newHostDetected) {
                    // start all hosts on equal terms
                    this.dbServerUtilisation.put(hostname, 0);
Logger.getAnonymousLogger().info("Resetted " + hostname);
                }
            }
            else {
                // a host providing the service has just disappeared
                this.dbServerUtilisation.remove(hostname);
Logger.getAnonymousLogger().info("Removed " + hostname);
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
