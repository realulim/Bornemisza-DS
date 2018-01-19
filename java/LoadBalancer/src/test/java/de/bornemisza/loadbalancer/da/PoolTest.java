package de.bornemisza.loadbalancer.da;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.OperationTimeoutException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.ClusterEvent.ClusterEventType;
import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.entity.PseudoHazelcastMap;
import de.bornemisza.loadbalancer.entity.PseudoHazelcastSet;

public class PoolTest {

    private HashMap<String, Object> allTestConnections;
    private HazelcastInstance hazelcast;
    private IMap utilisationMap;
    private ISet candidates;

    private final SecureRandom wheel = new SecureRandom();

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

        this.utilisationMap = new PseudoHazelcastMap();
        this.candidates = new PseudoHazelcastSet<>();
    }

    @Test
    public void hazelcastWorking() {
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        Pool CUT = new PoolImpl(hazelcast);
        List<String> dbServers = CUT.getDbServerQueue();
        assertEquals(CUT.getAllConnections().size(), dbServers.size());
        Map<String, Object> dbServerUtilisation = CUT.getDbServerUtilisation();
        assertEquals(utilisationMap, dbServerUtilisation);
        for (String hostname : allTestConnections.keySet()) {
            assertTrue(dbServers.contains(hostname));
            assertTrue(dbServerUtilisation.containsKey(hostname));
            assertEquals(0, dbServerUtilisation.get(hostname));
        }

        // subsequent invocations should just return the created data structures
        assertEquals(dbServers, CUT.getDbServerQueue());
        assertEquals(dbServerUtilisation, CUT.getDbServerUtilisation());
    }

    @Test
    public void hazelcastNotWorkingAtFirst_thenWorkingLater() {
        String errMsg = "Invocation failed to complete due to operation-heartbeat-timeout";
        when(hazelcast.getMap(Config.UTILISATION))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenReturn(utilisationMap);

        Pool CUT = new PoolImpl(hazelcast);
        Map dbServerUtilisation = CUT.getDbServerUtilisation(); // second invocation, still no Hazelcast
        assertTrue(utilisationMap.isEmpty());
        assertEquals(allTestConnections.size(), dbServerUtilisation.size()); // we have received a non-Hazelcast data structure as fallback

        // subsequent invocations should return the Hazelcast data structure
        dbServerUtilisation = CUT.getDbServerUtilisation();
        assertEquals(allTestConnections.size(), utilisationMap.size());
        assertEquals(utilisationMap, dbServerUtilisation);
    }

    @Test
    public void verifyRequestCounting() {
        IMap utilisationMapLocal = new PseudoHazelcastMap<>();
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMapLocal);
        Pool CUT = new PoolImpl(hazelcast);
        List<String> allHostnames = Arrays.asList(new String[] { "host1", "host2", "host3.hosts.de" });
        int requestCount = wheel.nextInt(15) + 1;
        for (String hostname : allHostnames) {
            utilisationMapLocal.put(hostname, 0);
            for (int i = 0; i < requestCount; i++) {
                CUT.trackUtilisation(hostname);
            }
        }
        for (String hostname : allHostnames) {
            assertEquals(requestCount, CUT.getDbServerUtilisation().get(hostname));
        }
        
    }

    @Test
    public void getDbServerQueue() throws Exception {
        IMap utilisationMapLocal = new PseudoHazelcastMap<>();
        for (int i = 0; i < 10; i++) {
            String randomKey = UUID.randomUUID().toString();
            utilisationMapLocal.put(randomKey, wheel.nextInt(100));
        }
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMapLocal);
        Pool CUT = new PoolImpl(hazelcast);
        List<String> sortedHostnames = CUT.getDbServerQueue();
        int lastUtilisation = 0;
        for (String hostname : sortedHostnames) {
            int utilisation = (Integer)utilisationMapLocal.get(hostname);
            assertTrue(lastUtilisation <= utilisation);
            lastUtilisation = utilisation;
        }
    }

    @Test
    public void candidateHealthy() {
        String candidate = "host-333.domain.de";
        when(hazelcast.getMap(Config.UTILISATION)).thenReturn(utilisationMap);
        when(hazelcast.getSet(Config.CANDIDATES)).thenReturn(candidates);
        candidates.add(candidate);
        Pool CUT = new PoolImpl(hazelcast);

        ClusterEvent clusterEvent = new ClusterEvent(candidate, ClusterEventType.CANDIDATE_HEALTHY);
        Message msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(clusterEvent);
        CUT.onMessage(msg);

        assertTrue(allTestConnections.containsKey(candidate));
        assertFalse(candidates.contains(candidate));
        assertEquals(allTestConnections.size(), utilisationMap.size());
        assertTrue(CUT.getDbServerUtilisation().keySet().contains(candidate));

        Map<String, Integer> utilisationMapLocal = CUT.getDbServerUtilisation();
        for (String hostname : utilisationMapLocal.keySet()) {
            assertEquals(0, (int)utilisationMapLocal.get(hostname));
        }
    }

    @Test
    public void removeHostFromService() {
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        for (String hostname : allTestConnections.keySet()) {
            utilisationMap.put(hostname, wheel.nextInt(100) + 1);
        }
        Pool CUT = new PoolImpl(hazelcast);
        assertEquals(allTestConnections.size(), CUT.getDbServerUtilisation().size());

        String removedHost = allTestConnections.keySet().iterator().next();
        ClusterEvent clusterEvent = new ClusterEvent(removedHost, ClusterEventType.HOST_DISAPPEARED);
        Message msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(clusterEvent);
        CUT.onMessage(msg);

        Map<String, Integer> utilisationMapLocal = CUT.getDbServerUtilisation();
        assertEquals(allTestConnections.size(), CUT.getDbServerUtilisation().size());
        assertFalse(allTestConnections.keySet().contains(removedHost));
        assertFalse(utilisationMapLocal.keySet().contains(removedHost));
    }

    @Test
    public void hostUnhealthy() {
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        for (String hostname : allTestConnections.keySet()) {
            utilisationMap.put(hostname, wheel.nextInt(100) + 1);
        }
        Pool CUT = new PoolImpl(hazelcast);
        assertEquals(allTestConnections.size(), CUT.getDbServerUtilisation().size());

        String unhealthyHost = allTestConnections.keySet().iterator().next();
        ClusterEvent clusterEvent = new ClusterEvent(unhealthyHost, ClusterEventType.HOST_UNHEALTHY);
        Message msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(clusterEvent);
        CUT.onMessage(msg);

        Map<String, Integer> utilisationMapLocal = CUT.getDbServerUtilisation();
        assertFalse(utilisationMapLocal.containsKey(unhealthyHost));
    }

    @Test
    public void hostUnhealthyThenHealthyAgain() {
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        for (String hostname : allTestConnections.keySet()) {
            utilisationMap.put(hostname, wheel.nextInt(100) + 1);
        }
        Pool CUT = new PoolImpl(hazelcast);
        assertEquals(allTestConnections.size(), CUT.getDbServerUtilisation().size());

        String hostname = allTestConnections.keySet().iterator().next();
        ClusterEvent unhealthyEvent = new ClusterEvent(hostname, ClusterEventType.HOST_UNHEALTHY);
        ClusterEvent healthyEvent = new ClusterEvent(hostname, ClusterEventType.HOST_HEALTHY);
        Message msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(unhealthyEvent).thenReturn(healthyEvent);
        CUT.onMessage(msg);

        Map<String, Integer> utilisationMapLocal = CUT.getDbServerUtilisation();
        assertFalse(utilisationMapLocal.containsKey(hostname));

        CUT.onMessage(msg);

        utilisationMapLocal = CUT.getDbServerUtilisation();
        for (int count : utilisationMapLocal.values()) {
            assertEquals(0, count);
        }
    }

    public class PoolImpl extends Pool {
        
        public PoolImpl(HazelcastInstance hazelcast) {
            super(hazelcast, mock(DnsProvider.class));
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
