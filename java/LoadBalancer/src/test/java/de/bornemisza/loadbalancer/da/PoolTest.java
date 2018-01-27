package de.bornemisza.loadbalancer.da;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.ClusterEvent.ClusterEventType;
import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.entity.PseudoHazelcastSet;
import de.bornemisza.loadbalancer.strategy.LoadBalancerStrategy;

public class PoolTest {

    private HashMap<String, Object> allTestConnections;
    private HazelcastInstance hazelcast;
    private LoadBalancerStrategy strategy;

    private ISet candidates;

    public PoolTest() {
    }

    @Before
    public void setUp() {
        this.allTestConnections = new HashMap<>();
        allTestConnections.put("host-1.domain.de", new Object());
        allTestConnections.put("host-2.domain.de", new Object());
        allTestConnections.put("host-3.domain.de", new Object());

        this.hazelcast = mock(HazelcastInstance.class);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(new HashSet<>());
        when(hazelcast.getCluster()).thenReturn(cluster);
        ITopic clusterMaintenanceTopic = mock(ITopic.class);
        when(hazelcast.getReliableTopic(anyString())).thenReturn(clusterMaintenanceTopic);
        strategy = mock(LoadBalancerStrategy.class);

        this.candidates = new PseudoHazelcastSet<>();
    }

    @Test
    public void candidateHealthy() {
        String candidate = "host-333.domain.de";
        when(hazelcast.getSet(Config.CANDIDATES)).thenReturn(candidates);
        candidates.add(candidate);
        Pool CUT = new PoolImpl(hazelcast, strategy);

        ClusterEvent clusterEvent = new ClusterEvent(candidate, ClusterEventType.CANDIDATE_HEALTHY);
        Message msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(clusterEvent);
        CUT.onMessage(msg);

        assertTrue(allTestConnections.containsKey(candidate));
        assertFalse(candidates.contains(candidate));
        verify(strategy).handleClusterEvent(clusterEvent);
    }

    @Test
    public void removeHostFromService() {
        Pool CUT = new PoolImpl(hazelcast, strategy);

        String removedHost = allTestConnections.keySet().iterator().next();
        ClusterEvent clusterEvent = new ClusterEvent(removedHost, ClusterEventType.HOST_DISAPPEARED);
        Message msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(clusterEvent);
        CUT.onMessage(msg);

        assertFalse(allTestConnections.keySet().contains(removedHost));
        verify(strategy).handleClusterEvent(clusterEvent);
    }

    @Test
    public void hostHealthy() {
        Pool CUT = new PoolImpl(hazelcast, strategy);

        String healthyHost = allTestConnections.keySet().iterator().next();
        ClusterEvent clusterEvent = new ClusterEvent(healthyHost, ClusterEventType.HOST_HEALTHY);
        Message msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(clusterEvent);
        CUT.onMessage(msg);

        verify(strategy).handleClusterEvent(clusterEvent);
    }

    @Test
    public void hostUnhealthy() {
        Pool CUT = new PoolImpl(hazelcast, strategy);

        String unhealthyHost = allTestConnections.keySet().iterator().next();
        ClusterEvent clusterEvent = new ClusterEvent(unhealthyHost, ClusterEventType.HOST_UNHEALTHY);
        Message msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(clusterEvent);
        CUT.onMessage(msg);

        verify(strategy).handleClusterEvent(clusterEvent);
    }

    @Test
    public void getNextDbServer_noUtilisation_noConnections() {
        when(strategy.getNextHost()).thenReturn(null);
        allTestConnections.clear();
        Pool CUT = new PoolImpl(hazelcast, strategy);
        try {
            CUT.getNextDbServer();
            fail();
        }
        catch (RuntimeException ex) {
            assertEquals("No DbServer available at all!", ex.getMessage());
        }
    }

    @Test
    public void getNextDbServer_noUtilisation() {
        when(strategy.getNextHost()).thenReturn(null);
        Pool CUT = new PoolImpl(hazelcast, strategy);
        String nextDbServer = CUT.getNextDbServer();
        assertTrue(allTestConnections.containsKey(nextDbServer));
    }

    @Test
    public void getNextDbServer() {
        String expected = "nextserver.com";
        when(strategy.getNextHost()).thenReturn(expected);
        Pool CUT = new PoolImpl(hazelcast, strategy);
        String nextDbServer = CUT.getNextDbServer();
        assertEquals(expected, nextDbServer);
    }

    public class PoolImpl extends Pool {
        
        public PoolImpl(HazelcastInstance hazelcast, LoadBalancerStrategy strategy) {
            super(hazelcast, strategy, mock(DnsProvider.class));
        }

        @Override
        protected String getServiceName() {
            return "someServiceName";
        }

        @Override
        protected Map<String, Object> createConnections() {
            return allTestConnections;
        }

        @Override
        protected Object createConnection(LoadBalancerConfig lbConfig, String hostname) {
            return new Object();
        }

        @Override
        protected LoadBalancerConfig getLoadBalancerConfig() {
            return mock(LoadBalancerConfig.class);
        }

    }
    
}
