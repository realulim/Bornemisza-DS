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

import de.bornemisza.loadbalancer.da.Pool;

@Singleton
@Startup
public class LoadBalancerPool {

    @Resource
    private TimerService timerService;

    private static final String TIMER_NAME = "LoadBalancerPoolTimer";

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    HealthChecks healthChecks;

    private Map<String, Integer> couchDbHostUtilisation;
    private List<String> couchDbHostQueue;

    public LoadBalancerPool() {
    }

    // Constructor for Unit Tests
    LoadBalancerPool(HazelcastInstance hz) {
        this.hazelcast = hz;
    }

    // Constructor for Unit Tests
    LoadBalancerPool(Map<String, Integer> utilisationMap) {
        this.couchDbHostUtilisation = utilisationMap;
    }

    // Constructor for Unit Tests
    LoadBalancerPool(List<String> hostQueue, HealthChecks checks) {
        this.couchDbHostQueue = hostQueue;
        this.healthChecks = checks;
    }

    @PostConstruct
    public void init() {
        couchDbHostUtilisation = hazelcast.getMap(Pool.MAP_COUCHDB_UTILISATION);
        couchDbHostQueue = hazelcast.getList(Pool.LIST_COUCHDB_HOSTQUEUE);
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
        return this.couchDbHostUtilisation.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue())
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    void updateQueue(List<String> sortedHostnames) {
        for (String hostname : sortedHostnames) {
            if (healthChecks.isHostAvailable(hostname, 443)) {
                if (couchDbHostQueue.contains(hostname)) {
                    couchDbHostQueue.add(hostname);
                    couchDbHostQueue.remove(hostname); // first occurrence, i. e. previous position in list
                }
                else {
                    couchDbHostQueue.add(hostname);
                }
            }
            else {
                couchDbHostQueue.remove(hostname); // stale
                
            }
        }
    }

    private void logNewQueueState() {
        StringBuilder sb = new StringBuilder("CouchDbHostQueue");
        for (String hostname : couchDbHostQueue) {
            sb.append(" | ").append(hostname).append(":").append(couchDbHostUtilisation.get(hostname));
        }
        Logger.getAnonymousLogger().info(sb.toString());
    }

}
