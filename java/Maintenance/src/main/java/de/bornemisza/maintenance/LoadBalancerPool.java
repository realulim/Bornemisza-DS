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

@Singleton
@Startup
public class LoadBalancerPool {

    @Resource
    private TimerService timerService;

    private static final String TIMER_NAME = "LoadBalancerPoolTimer";

    @Inject
    HazelcastInstance hazelcast;

    private Map<String, Integer> dbServerUtilisation;
    private List<String> dbServers;

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
        dbServers = hazelcast.getList(Config.SERVERS);
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
        List<String> sortedHostnames = sortHostnamesByUtilisation();

        try {
            String service = getDatabaseServiceName();
            List<String> dnsHostnames = DnsProvider.getHostnamesForService(service);
            updateDbServerUtilisation(sortedHostnames, dnsHostnames);
        }
        catch (NamingException ex) {
            Logger.getAnonymousLogger().warning("Problem reading SRV-Records: " + ex.toString());
        }

        logNewQueueState();
    }

    List<String> sortHostnamesByUtilisation() {
        return this.dbServerUtilisation.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue())
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    void updateDbServerUtilisation(List<String> sortedHostnames, List<String> dnsHostnames) {
        for (String dnsHostname : dnsHostnames) {
            if (! sortedHostnames.contains(dnsHostname)) {
                // a new host providing the service just appeared
                this.dbServerUtilisation.putIfAbsent(dnsHostname, 0);
            }
        }
        for (String hostname : sortedHostnames) {
            if (! dnsHostnames.contains(hostname)) {
                // a host providing the service has just disappeared
                this.dbServerUtilisation.remove(hostname);
            }
        }
    }

    private void logNewQueueState() {
        StringBuilder sb = new StringBuilder("DbServerQueue");
        for (String hostname : dbServers) {
            sb.append(" | ").append(hostname).append(":").append(dbServerUtilisation.get(hostname));
        }
        Logger.getAnonymousLogger().info(sb.toString());
    }

    protected String getDatabaseServiceName() {
        return "_db._tcp.bornemisza.de.";
    }

}
