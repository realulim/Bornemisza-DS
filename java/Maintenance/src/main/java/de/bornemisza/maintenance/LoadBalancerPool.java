package de.bornemisza.maintenance;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.naming.NamingException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.rest.da.HttpPool;

@Singleton
@Startup
public class LoadBalancerPool {

    @Resource(name="http/Base")
    HttpPool pool;

    @Resource
    private TimerService timerService;

    private static final String TIMER_NAME = "LoadBalancerPoolTimer";

    @Inject
    HazelcastInstance hazelcast;

    private Map<String, Integer> dbServerUtilisation;

    public LoadBalancerPool() {
    }

    // Constructor for Unit Tests
    LoadBalancerPool(HazelcastInstance hz, Map<String, Integer> utilisationMap) {
        this.hazelcast = hz;
        this.dbServerUtilisation = utilisationMap;
    }

    @PostConstruct
    public void init() {
        dbServerUtilisation = hazelcast.getMap(Config.UTILISATION);
        hazelcast.getCluster().addMembershipListener(new MembershipListener() {
            @Override public void memberAdded(MembershipEvent me) { rebuildTimer(); }
            @Override public void memberRemoved(MembershipEvent me) { rebuildTimer(); }
            @Override public void memberAttributeChanged(MemberAttributeEvent mae) { }
        });
        rebuildTimer();
    }

    void rebuildTimer() {
        Collection<Timer> timers = timerService.getAllTimers();
        for (Timer timer: timers) {
            try {
                if (timer.getInfo().equals(TIMER_NAME)) {
                    timer.cancel();
                }
            }
            catch (IllegalStateException | NoSuchObjectLocalException e) {
                Logger.getAnonymousLogger().warning("Timer inaccessible: " + timer);
            }
        }
        ScheduleExpression expression = new ScheduleExpression();
        expression.hour("*").minute(calculateMinuteExpression());
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(TIMER_NAME);
        timerService.createCalendarTimer(expression, timerConfig);
        Logger.getAnonymousLogger().info("Installed Timer with " + expression.toString());
    }

    String calculateMinuteExpression() {
        Set<Member> members = hazelcast.getCluster().getMembers();
        List<String> sortedUuids = members.stream()
                .map(member -> member.getUuid())
                .sorted(Comparator.<String>naturalOrder())
                .collect(Collectors.toList());
        String myself = hazelcast.getCluster().getLocalMember().getUuid();
        return sortedUuids.indexOf(myself) + "/" + members.size();
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
        boolean reset = false;
        for (String dnsHostname : dnsHostnames) {
            if (! utilisedHostnames.contains(dnsHostname)) {
                // a new host providing the service just appeared
                this.dbServerUtilisation.putIfAbsent(dnsHostname, 0);
                reset = true; // start all hosts on equal terms
            }
        }
        for (String hostname : utilisedHostnames) {
            if (dnsHostnames.contains(hostname)) {
                if (reset) this.dbServerUtilisation.put(hostname, 0);
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
