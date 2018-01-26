package de.bornemisza.loadbalancer.strategy;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.OperationTimeoutException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.ClusterEvent.ClusterEventType;
import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.entity.PseudoHazelcastMap;

public class UtilisationStrategyTest {
    
    private HazelcastInstance hazelcast;
    private IMap utilisationMap;

    private final SecureRandom wheel = new SecureRandom();

    public UtilisationStrategyTest() {
    }

    @Before
    public void setUp() {
        this.hazelcast = mock(HazelcastInstance.class);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(new HashSet<>());
        when(hazelcast.getCluster()).thenReturn(cluster);
        ITopic clusterMaintenanceTopic = mock(ITopic.class);
        when(hazelcast.getReliableTopic(anyString())).thenReturn(clusterMaintenanceTopic);

        this.utilisationMap = new PseudoHazelcastMap();
    }

    @Test
    public void getNextHost() {
        IMap utilisationMapLocal = new PseudoHazelcastMap<>();
        for (int i = 0; i < 10; i++) {
            String randomKey = UUID.randomUUID().toString();
            utilisationMapLocal.put(randomKey, wheel.nextInt(100));
        }
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMapLocal);

        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);
        assertEquals(CUT.getNextHost(), CUT.getHostQueue().iterator().next());

        utilisationMapLocal.clear();
        assertNull(CUT.getNextHost());
        assertEquals(0, CUT.getHostQueue().size());
    }

    @Test
    public void hazelcastWorking() {
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);
        List<String> hosts = CUT.getHostQueue();
        Map<String, Integer> hostUtilisation = CUT.getHostUtilisation();
        assertEquals(utilisationMap, hostUtilisation);

        // subsequent invocations should just return the created data structures
        assertEquals(hosts, CUT.getHostQueue());
        assertEquals(hostUtilisation, CUT.getHostUtilisation());
    }

    @Test
    public void hazelcastNotWorkingAtFirst_thenWorkingLater() {
        String errMsg = "Invocation failed to complete due to operation-heartbeat-timeout";
        when(hazelcast.getMap(Config.UTILISATION))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenReturn(utilisationMap);

        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);
        Map dbServerUtilisation = CUT.getHostUtilisation(); // second invocation, still no Hazelcast
        assertTrue(utilisationMap.isEmpty());
        assertEquals(0, dbServerUtilisation.size()); // we have received a non-Hazelcast data structure as fallback

        // subsequent invocations should return the Hazelcast data structure
        dbServerUtilisation = CUT.getHostUtilisation();
        assertEquals(utilisationMap, dbServerUtilisation);
    }

    @Test
    public void verifyRequestCounting() {
        IMap utilisationMapLocal = new PseudoHazelcastMap<>();
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMapLocal);
        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);
        List<String> allHostnames = Arrays.asList(new String[] { "host1", "host2", "host3.hosts.de" });
        int requestCount = wheel.nextInt(15) + 1;
        for (String hostname : allHostnames) {
            utilisationMapLocal.put(hostname, 0);
            for (int i = 0; i < requestCount; i++) {
                CUT.trackUsage(hostname);
            }
        }
        for (String hostname : allHostnames) {
            assertEquals(requestCount, CUT.getHostUtilisation().get(hostname).intValue());
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
        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);
        List<String> sortedHostnames = CUT.getHostQueue();
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
        utilisationMap.put("host1.domain.de", wheel.nextInt(100) + 1);
        utilisationMap.put("host2.domain.de", wheel.nextInt(100) + 1);
        utilisationMap.put("host3.domain.de", wheel.nextInt(100) + 1);
        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);

        ClusterEvent clusterEvent = new ClusterEvent(candidate, ClusterEventType.CANDIDATE_HEALTHY);
        CUT.handleClusterEvent(clusterEvent);

        assertTrue(CUT.getHostUtilisation().keySet().contains(candidate));

        Map<String, Integer> utilisationMapLocal = CUT.getHostUtilisation();
        for (String hostname : utilisationMapLocal.keySet()) {
            assertEquals(0, (int)utilisationMapLocal.get(hostname));
        }
    }

    @Test
    public void removeHostFromService() {
        String removedHost = "host-444.domain.de";
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        utilisationMap.put(removedHost, wheel.nextInt(100) + 1);
        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);

        ClusterEvent clusterEvent = new ClusterEvent(removedHost, ClusterEventType.HOST_DISAPPEARED);
        CUT.handleClusterEvent(clusterEvent);

        Map<String, Integer> utilisationMapLocal = CUT.getHostUtilisation();
        assertFalse(utilisationMapLocal.keySet().contains(removedHost));
    }

    @Test
    public void hostHealthy() {
        String healthyHost = "host-222.domain.de";
        when(hazelcast.getMap(Config.UTILISATION)).thenReturn(utilisationMap);
        utilisationMap.put("host1.domain.de", wheel.nextInt(100) + 1);
        utilisationMap.put("host2.domain.de", wheel.nextInt(100) + 1);
        utilisationMap.put("host3.domain.de", wheel.nextInt(100) + 1);
        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);

        ClusterEvent clusterEvent = new ClusterEvent(healthyHost, ClusterEventType.HOST_HEALTHY);
        CUT.handleClusterEvent(clusterEvent);

        assertTrue(CUT.getHostUtilisation().keySet().contains(healthyHost));

        Map<String, Integer> utilisationMapLocal = CUT.getHostUtilisation();
        for (String hostname : utilisationMapLocal.keySet()) {
            assertEquals(0, (int)utilisationMapLocal.get(hostname));
        }
    }

    @Test
    public void hostUnhealthy() {
        String unhealthyHost = "host-555.domain.de";
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        utilisationMap.put(unhealthyHost, wheel.nextInt(100) + 1);
        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);

        ClusterEvent clusterEvent = new ClusterEvent(unhealthyHost, ClusterEventType.HOST_UNHEALTHY);
        CUT.handleClusterEvent(clusterEvent);

        Map<String, Integer> utilisationMapLocal = CUT.getHostUtilisation();
        assertFalse(utilisationMapLocal.containsKey(unhealthyHost));
    }

    @Test
    public void hostUnhealthyThenHealthyAgain() {
        String hostname = "host-666.domain.de";
        utilisationMap.put(hostname, wheel.nextInt(100) + 1);
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);

        UtilisationStrategy CUT = new UtilisationStrategy(hazelcast);

        ClusterEvent unhealthyEvent = new ClusterEvent(hostname, ClusterEventType.HOST_UNHEALTHY);
        ClusterEvent healthyEvent = new ClusterEvent(hostname, ClusterEventType.HOST_HEALTHY);
        CUT.handleClusterEvent(unhealthyEvent);

        Map<String, Integer> utilisationMapLocal = CUT.getHostUtilisation();
        assertFalse(utilisationMapLocal.containsKey(hostname));

        CUT.handleClusterEvent(healthyEvent);

        utilisationMapLocal = CUT.getHostUtilisation();
        for (int count : utilisationMapLocal.values()) {
            assertEquals(0, count);
        }
    }
    
}
