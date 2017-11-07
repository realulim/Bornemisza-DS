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

import de.bornemisza.maintenance.task.HealthCheckTask;
import de.bornemisza.maintenance.task.SrvRecordsTask;

@Startup
@Singleton
public class Scheduler {

    private static final String SRV_RECORDS_TASK_TIMER_NAME = "SrvRecordsTaskTimer";
    private static final String HEALTH_CHECK_TASK_TIMER_NAME = "HealthCheckTaskTimer";

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    SrvRecordsTask srvRecordsTask;

    @Inject
    HealthCheckTask healthCheckTask;

    private Timer srvRecordsTaskTimer = null;
    private Timer healthCheckTaskTimer = null;

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
        if (srvRecordsTaskTimer == null || healthCheckTaskTimer == null) {
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
            if (healthCheckTaskTimer != null) {
                healthCheckTaskTimer.cancel();
                Logger.getAnonymousLogger().info("Cancelled Timer " + HEALTH_CHECK_TASK_TIMER_NAME);
            }
            createSrvRecordsTaskTimer();
            createHealthCheckTaskTimer();
        }
        catch (IllegalStateException | NoSuchObjectLocalException e) {
            Logger.getAnonymousLogger().severe("Timer inaccessible: " + e.toString());
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

    private void createHealthCheckTaskTimer() {
        String seconds = calculateSecondExpression();
        if (seconds != null) {
            ScheduleExpression expression = new ScheduleExpression();
            expression.hour("*").minute("*").second(calculateSecondExpression());
            TimerConfig timerConfig = new TimerConfig();
            timerConfig.setPersistent(false);
            timerConfig.setInfo(HEALTH_CHECK_TASK_TIMER_NAME);
            this.healthCheckTaskTimer = healthCheckTask.createTimer(expression, timerConfig);
            Logger.getAnonymousLogger().info("Installed Timer " + HEALTH_CHECK_TASK_TIMER_NAME + " with " + expression.toString());
        }
        else {
            Logger.getAnonymousLogger().info("Skipping Timer " + HEALTH_CHECK_TASK_TIMER_NAME + ", there are already enough of them.");
        }
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
        int clusterSize = members.size();
        List<String> sortedUuids = members.stream()
                .map(member -> member.getUuid())
                .sorted(Comparator.<String>naturalOrder())
                .collect(Collectors.toList());
        String myself = hazelcast.getCluster().getLocalMember().getUuid();
        int myIndex = sortedUuids.indexOf(myself);
        if (myIndex < 6) {
            if ((myIndex + clusterSize) < 6) {
                return myIndex * 10 + "/" + clusterSize * 10;
            }
            else return myIndex * 10 + "";
        }
        else return null;
    }

}
