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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import de.bornemisza.loadbalancer.Config;


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
    LoadBalancerPool(HazelcastInstance hz) {
        this.hazelcast = hz;
    }

    // Constructor for Unit Tests
    LoadBalancerPool(Map<String, Integer> utilisationMap) {
        this.dbServerUtilisation = utilisationMap;
    }

    // Constructor for Unit Tests
    LoadBalancerPool(List<String> dbServers) {
        this.dbServers = dbServers;
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
    public void checkPool() {
        List<String> sortedHostnames = sortHostnamesByUtilisation();

        updateQueue(sortedHostnames);

        logNewQueueState();
    }

    List<String> sortHostnamesByUtilisation() {
        return this.dbServerUtilisation.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue())
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    void updateQueue(List<String> sortedHostnames) {
        dbServers.addAll(0, sortedHostnames); // add at start of queue
        if (dbServers.size() > sortedHostnames.size()) {
            // remove extraneous elements from end of queue
            dbServers.subList(sortedHostnames.size(), dbServers.size()).clear();
        }
    }

    private void logNewQueueState() {
        StringBuilder sb = new StringBuilder("DbServerQueue");
        for (String hostname : dbServers) {
            sb.append(" | ").append(hostname).append(":").append(dbServerUtilisation.get(hostname));
        }
        Logger.getAnonymousLogger().info(sb.toString());
    }

}
