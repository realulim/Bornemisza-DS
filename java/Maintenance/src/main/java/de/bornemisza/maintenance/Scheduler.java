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

    private static final String SRV_RECORDS_TASK_TIMER_NAME = "SrvRecordsTaskTimer";

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    SrvRecordsTask srvRecordsTask;

    private Timer srvRecordsTaskTimer = null;

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
        if (srvRecordsTaskTimer == null) {
            // don't do it again, if already invoked by callback
            rebuildTimers();
        }
    }

    void rebuildTimers() {
        try {
            if (srvRecordsTaskTimer != null) {
                srvRecordsTaskTimer.cancel();
                Logger.getAnonymousLogger().info("Cancelled Timer " + SRV_RECORDS_TASK_TIMER_NAME);
            }
            createSrvRecordsTaskTimer();
        }
        catch (IllegalStateException | NoSuchObjectLocalException e) {
            Logger.getAnonymousLogger().severe("Timer " + srvRecordsTaskTimer + " inaccessible: " + e.toString());
        }
    }

    private void createSrvRecordsTaskTimer() {
        ScheduleExpression expression = new ScheduleExpression();
        expression.hour("*").minute(calculateMinuteExpression());
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(SRV_RECORDS_TASK_TIMER_NAME);
        this.srvRecordsTaskTimer = srvRecordsTask.createTimer(expression, timerConfig);
        Logger.getAnonymousLogger().info("Installed Timer " + SRV_RECORDS_TASK_TIMER_NAME + " with " + expression.toString());
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

    String calculateSecondExpression() {
        Set<Member> members = hazelcast.getCluster().getMembers();
        List<String> sortedUuids = members.stream()
                .map(member -> member.getUuid())
                .sorted(Comparator.<String>naturalOrder())
                .collect(Collectors.toList());
        String myself = hazelcast.getCluster().getLocalMember().getUuid();
        int myIndex = sortedUuids.indexOf(myself);
        if (myIndex < 6) {
            return "" + (myIndex * 10);
        }
        else return null;
    }

}
