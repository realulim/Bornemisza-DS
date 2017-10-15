package de.bornemisza.maintenance;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.inject.Inject;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

@Startup
@Singleton
public class Scheduler {

    private static final String TIMER_NAME = "LoadBalancerTaskTimer";

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    LoadBalancerTask loadBalancerTask;

    private Timer loadBalancerTaskTimer = null;

    public Scheduler() {
    }

    // Constructor for Unit Tests
    public Scheduler(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @PostConstruct
    public void init() {
        hazelcast.getCluster().addMembershipListener(new MembershipListener() {
            @Override public void memberAdded(MembershipEvent me) { rebuildTimers(); }
            @Override public void memberRemoved(MembershipEvent me) { rebuildTimers(); }
            @Override public void memberAttributeChanged(MemberAttributeEvent mae) { }
        });
        if (loadBalancerTask == null) {
            // don't do it again, if already invoked by callback
            rebuildTimers();
        }
    }

    void rebuildTimers() {
        try {
            if (loadBalancerTaskTimer != null) {
                loadBalancerTaskTimer.cancel();
                Logger.getAnonymousLogger().info("Cancelled Timer " + TIMER_NAME);
            }
            createLoadBalancerTaskTimer();
        }
        catch (IllegalStateException | NoSuchObjectLocalException e) {
            Logger.getAnonymousLogger().severe("Timer " + loadBalancerTaskTimer + " inaccessible: " + e.toString());
        }
    }

    private void createLoadBalancerTaskTimer() {
        ScheduleExpression expression = new ScheduleExpression();
        expression.hour("*").minute(calculateMinuteExpression());
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(TIMER_NAME);
        this.loadBalancerTaskTimer = loadBalancerTask.createTimer(expression, timerConfig);
        Logger.getAnonymousLogger().info("Installed Timer " + TIMER_NAME + " with " + expression.toString());
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

}
