package de.bornemisza.maintenance;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private Cluster cluster;
    private Scheduler CUT;

    public SchedulerTest() {
    }

    @Before
    public void setUp() {
        this.wheel = new SecureRandom();
        hazelcast = mock(HazelcastInstance.class);
        cluster = mock(Cluster.class);
        when(hazelcast.getCluster()).thenReturn(cluster);
        CUT = new Scheduler(hazelcast);
    }

    @Test
    public void calculateMinuteExpression() {
        Set<Member> members = new HashSet<>();
        Member member = null;
        for (int i = 0; i <= wheel.nextInt(10); i++) {
            member = mock(Member.class);
            when(member.getUuid()).thenReturn(UUID.randomUUID().toString());
            members.add(member);
        }
        when(cluster.getLocalMember()).thenReturn(member);
        when(cluster.getMembers()).thenReturn(members);
        String expr = CUT.calculateMinuteExpression();
        assertTrue(Character.getNumericValue(expr.charAt(0)) < members.size());
        assertEquals("/", String.valueOf(expr.charAt(1)));
        assertEquals(members.size(), Character.getNumericValue(expr.charAt(2)));
        System.out.println(expr);
    }

    @Test
    public void calculateSecondExpression_oneMember() {
        List<Member> members = createMembers(1);
        when(cluster.getLocalMember()).thenReturn(members.get(0));
        when(cluster.getMembers()).thenReturn(new HashSet(members));
        String expr = CUT.calculateSecondExpression();
        assertEquals("0/10", expr);
    }

    @Test
    public void calculateSecondExpression_twoMembers() {
        List<Member> members = createMembers(2);
        when(cluster.getLocalMember()).thenReturn(members.get(0)).thenReturn(members.get(1));
        when(cluster.getMembers()).thenReturn(new HashSet(members));
        String expr = CUT.calculateSecondExpression();
        assertEquals("0/20", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("10/20", expr);
    }

    @Test
    public void calculateSecondExpression_threeMembers() {
        List<Member> members = createMembers(3);
        when(cluster.getLocalMember()).thenReturn(members.get(0)).thenReturn(members.get(1)).thenReturn(members.get(2));
        when(cluster.getMembers()).thenReturn(new HashSet(members));
        String expr = CUT.calculateSecondExpression();
        assertEquals("0/30", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("10/30", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("20/30", expr);
    }

    @Test
    public void calculateSecondExpression_fourMembers() {
        List<Member> members = createMembers(4);
        when(cluster.getLocalMember()).thenReturn(members.get(0)).thenReturn(members.get(1)).thenReturn(members.get(2)).thenReturn(members.get(3));
        when(cluster.getMembers()).thenReturn(new HashSet(members));
        String expr = CUT.calculateSecondExpression();
        assertEquals("0/40", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("10/40", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("20", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("30", expr);
    }

    @Test
    public void calculateSecondExpression_fiveMembers() {
        List<Member> members = createMembers(5);
        when(cluster.getLocalMember()).thenReturn(members.get(0)).thenReturn(members.get(1)).thenReturn(members.get(2)).thenReturn(members.get(3)).thenReturn(members.get(4));
        when(cluster.getMembers()).thenReturn(new HashSet(members));
        String expr = CUT.calculateSecondExpression();
        assertEquals("0/50", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("10", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("20", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("30", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("40", expr);
    }

    @Test
    public void calculateSecondExpression_sixMembers() {
        List<Member> members = createMembers(6);
        when(cluster.getLocalMember()).thenReturn(members.get(0)).thenReturn(members.get(1)).thenReturn(members.get(2)).thenReturn(members.get(3)).thenReturn(members.get(4)).thenReturn(members.get(5));
        when(cluster.getMembers()).thenReturn(new HashSet(members));
        String expr = CUT.calculateSecondExpression();
        assertEquals("0", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("10", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("20", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("30", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("40", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("50", expr);
    }

    @Test
    public void calculateSecondExpression_sevenMembers() {
        List<Member> members = createMembers(7);
        when(cluster.getLocalMember()).thenReturn(members.get(0)).thenReturn(members.get(1)).thenReturn(members.get(2)).thenReturn(members.get(3)).thenReturn(members.get(4)).thenReturn(members.get(5)).thenReturn(members.get(6));
        when(cluster.getMembers()).thenReturn(new HashSet(members));
        String expr = CUT.calculateSecondExpression();
        assertEquals("0", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("10", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("20", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("30", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("40", expr);
        expr = CUT.calculateSecondExpression();
        assertEquals("50", expr);
        assertNull(CUT.calculateSecondExpression());
    }

    private List<Member> createMembers(int count) {
        List<Member> members = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Member member = mock(Member.class);
            when(member.getUuid()).thenReturn("UUID-" + i);
            members.add(member);
        }
        return members;
    }

}
