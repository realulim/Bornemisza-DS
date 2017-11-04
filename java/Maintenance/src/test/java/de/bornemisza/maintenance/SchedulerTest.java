package de.bornemisza.maintenance;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

public class SchedulerTest {

    private SecureRandom wheel;
    private HazelcastInstance hazelcast;

    public SchedulerTest() {
    }

    @Before
    public void setUp() {
        this.wheel = new SecureRandom();
        hazelcast = mock(HazelcastInstance.class);
    }

    @Test
    public void calculateMinuteExpression() {
        Cluster cluster = mock(Cluster.class);
        when(hazelcast.getCluster()).thenReturn(cluster);
        Set<Member> members = new HashSet<>();
        Member member = null;
        for (int i = 0; i <= wheel.nextInt(10); i++) {
            member = mock(Member.class);
            when(member.getUuid()).thenReturn(UUID.randomUUID().toString());
            members.add(member);
        }
        when(cluster.getLocalMember()).thenReturn(member);
        when(cluster.getMembers()).thenReturn(members);
        Scheduler CUT = new Scheduler(hazelcast);
        String expr = CUT.calculateMinuteExpression();
        assertTrue(Character.getNumericValue(expr.charAt(0)) < members.size());
        assertEquals("/", String.valueOf(expr.charAt(1)));
        assertEquals(members.size(), Character.getNumericValue(expr.charAt(2)));
    }

    @Test
    public void calculateSecondExpression_memberAmongFirstSix() {
        Cluster cluster = mock(Cluster.class);
        when(hazelcast.getCluster()).thenReturn(cluster);
        Set<Member> members = new HashSet<>();
        Member member = null;
        int count = wheel.nextInt(6);
        for (int i = 0; i <= count; i++) {
            member = mock(Member.class);
            when(member.getUuid()).thenReturn("ABC" + i);
            members.add(member);
        }
        when(cluster.getLocalMember()).thenReturn(member);
        when(cluster.getMembers()).thenReturn(members);
        Scheduler CUT = new Scheduler(hazelcast);
        String expr = CUT.calculateSecondExpression();
        assertEquals(expr, "" + (count * 10));
    }

    @Test
    public void calculateSecondExpression_memberNotAmongFirstSix() {
        Cluster cluster = mock(Cluster.class);
        when(hazelcast.getCluster()).thenReturn(cluster);
        Set<Member> members = new HashSet<>();
        Member member = null;
        for (int i = 0; i < 10; i++) {
            member = mock(Member.class);
            when(member.getUuid()).thenReturn("ABC" + i);
            members.add(member);
        }
        when(cluster.getLocalMember()).thenReturn(member);
        when(cluster.getMembers()).thenReturn(members);
        Scheduler CUT = new Scheduler(hazelcast);
        String expr = CUT.calculateSecondExpression();
        assertNull(expr);
    }

}
