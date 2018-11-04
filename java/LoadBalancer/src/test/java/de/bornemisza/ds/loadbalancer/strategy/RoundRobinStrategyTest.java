package de.bornemisza.ds.loadbalancer.strategy;

import de.bornemisza.ds.loadbalancer.strategy.RoundRobinStrategy;
import java.security.SecureRandom;

import com.hazelcast.core.HazelcastInstance;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.ds.loadbalancer.ClusterEvent;
import de.bornemisza.ds.loadbalancer.ClusterEvent.ClusterEventType;
import de.bornemisza.ds.loadbalancer.Config;
import de.bornemisza.ds.loadbalancer.entity.PseudoHazelcastMap;

public class RoundRobinStrategyTest {

    private HazelcastInstance hazelcast;
    private PseudoHazelcastMap utilisationMap;
    private final SecureRandom wheel = new SecureRandom();
    
    @Before
    public void setUp() {
        hazelcast = mock(HazelcastInstance.class);
        utilisationMap = new PseudoHazelcastMap();
    }

    @Test
    public void getNextHost_noUtilisation() {
        when(hazelcast.getMap(Config.UTILISATION)).thenReturn(utilisationMap);
        RoundRobinStrategy CUT = new RoundRobinStrategy(hazelcast);
        assertNull(CUT.getNextHost());
    }

    @Test
    public void getNextHost() {
        utilisationMap.put("host1", 12);
        utilisationMap.put("host2", 13);
        utilisationMap.put("host3", 14);
        when(hazelcast.getMap(Config.UTILISATION)).thenReturn(utilisationMap);
        RoundRobinStrategy CUT = new RoundRobinStrategy(hazelcast);
        assertEquals("host1", CUT.getNextHost());
        assertEquals("host2", CUT.getNextHost());
        assertEquals("host3", CUT.getNextHost());
        assertEquals("host1", CUT.getNextHost());
        assertEquals("host2", CUT.getNextHost());
        assertEquals("host3", CUT.getNextHost());
        assertEquals(CUT.getHostQueue().get(0), CUT.getNextHost());
    }

    @Test
    public void candidateOrHostHealthy() {
        String hostOrCandidate = "host-333.domain.de";
        when(hazelcast.getMap(Config.UTILISATION)).thenReturn(utilisationMap);
        utilisationMap.put("host1.domain.de", wheel.nextInt(100) + 1);
        utilisationMap.put("host2.domain.de", wheel.nextInt(100) + 1);
        utilisationMap.put("host3.domain.de", wheel.nextInt(100) + 1);
        RoundRobinStrategy CUT = new RoundRobinStrategy(hazelcast);

        ClusterEvent clusterEvent = new ClusterEvent(hostOrCandidate, ClusterEventType.CANDIDATE_HEALTHY);
        CUT.handleClusterEvent(clusterEvent);

        assertTrue(CUT.getHostQueue().contains(hostOrCandidate));
        CUT.getHostQueue().remove(hostOrCandidate);

        clusterEvent = new ClusterEvent(hostOrCandidate, ClusterEventType.HOST_HEALTHY);
        CUT.handleClusterEvent(clusterEvent);

        assertTrue(CUT.getHostQueue().contains(hostOrCandidate));
    }

    @Test
    public void hostUnhealthyOrDisappeared() {
        String unhealthyOrDisappearedHost = "host-444.domain.de";
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        utilisationMap.put(unhealthyOrDisappearedHost, wheel.nextInt(100) + 1);
        RoundRobinStrategy CUT = new RoundRobinStrategy(hazelcast);

        ClusterEvent clusterEvent = new ClusterEvent(unhealthyOrDisappearedHost, ClusterEventType.HOST_DISAPPEARED);
        CUT.handleClusterEvent(clusterEvent);

        assertFalse(CUT.getHostQueue().contains(unhealthyOrDisappearedHost));
        utilisationMap.put(unhealthyOrDisappearedHost, wheel.nextInt(100) + 1);

        clusterEvent = new ClusterEvent(unhealthyOrDisappearedHost, ClusterEventType.HOST_UNHEALTHY);
        CUT.handleClusterEvent(clusterEvent);

        assertFalse(CUT.getHostQueue().contains(unhealthyOrDisappearedHost));
    }

}
