package de.bornemisza.loadbalancer.da;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.OperationTimeoutException;

import de.bornemisza.loadbalancer.entity.PseudoHazelcastMap;

public class PoolTest {

    private HashMap<String, Object> allConnections;
    private HazelcastInstance hazelcast;
    private IMap hazelcastMap;

    private final SecureRandom wheel = new SecureRandom();

    public PoolTest() {
    }

    @Before
    public void setUp() {
        this.allConnections = new HashMap<>();
        allConnections.put("host-1.domain.de", new Object());
        allConnections.put("host-2.domain.de", new Object());
        allConnections.put("host-3.domain.de", new Object());

        this.hazelcast = mock(HazelcastInstance.class);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(new HashSet<>());
        when(hazelcast.getCluster()).thenReturn(cluster);
        this.hazelcastMap = new PseudoHazelcastMap();
    }

    //@Test
    public void hazelcastWorking() {
        when(hazelcast.getMap(anyString())).thenReturn(hazelcastMap);
        Pool CUT = new PoolImpl(allConnections, hazelcast);
        List<String> dbServers = CUT.getDbServerQueue();
        assertEquals(CUT.getAllHostnames().size(), dbServers.size());
        Map<String, Object> dbServerUtilisation = CUT.getDbServerUtilisation();
        assertEquals(hazelcastMap, dbServerUtilisation);
        for (String hostname : allConnections.keySet()) {
            assertTrue(dbServers.contains(hostname));
            assertTrue(dbServerUtilisation.containsKey(hostname));
            assertEquals(0, dbServerUtilisation.get(hostname));
        }

        // subsequent invocations should just return the created data structures
        assertEquals(dbServers, CUT.getDbServerQueue());
        assertEquals(dbServerUtilisation, CUT.getDbServerUtilisation());
    }

    //@Test
    public void verifyRequestCounting() {
        IMap utilisationMap = new PseudoHazelcastMap<>();
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        Pool CUT = new PoolImpl(allConnections, hazelcast);
        List<String> allHostnames = Arrays.asList(new String[] { "host1", "host2", "host3.hosts.de" });
        int requestCount = wheel.nextInt(15) + 1;
        for (String hostname : allHostnames) {
            utilisationMap.put(hostname, 0);
            for (int i = 0; i < requestCount; i++) {
                CUT.trackUtilisation(hostname);
            }
        }
        for (String hostname : allHostnames) {
            assertEquals(requestCount, CUT.getDbServerUtilisation().get(hostname));
        }
        
    }

    //@Test
    public void hazelcastNotWorkingAtFirst_thenWorkingLater() {
        String errMsg = "Invocation failed to complete due to operation-heartbeat-timeout";
        when(hazelcast.getMap(anyString()))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenReturn(hazelcastMap);

        Pool CUT = new PoolImpl(allConnections, hazelcast);
        Map dbServerUtilisation = CUT.getDbServerUtilisation(); // second invocation, still no Hazelcast
        assertTrue(hazelcastMap.isEmpty());
        assertEquals(allConnections.size(), dbServerUtilisation.size()); // we have received a non-Hazelcast data structure as fallback

        // subsequent invocations should return the Hazelcast data structure
        dbServerUtilisation = CUT.getDbServerUtilisation();
        assertEquals(allConnections.size(), hazelcastMap.size());
        assertEquals(hazelcastMap, dbServerUtilisation);
    }

    @Test
    public void verifyConsistencyBetweenQueueAndAllHostnames() {
        IMap utilisationMap = new PseudoHazelcastMap<>();
        for (String key : allConnections.keySet()) {
            utilisationMap.put(key, 12);
        }
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        Pool CUT = new PoolImpl(allConnections, hazelcast);
        List<String> dbServerQueue = CUT.getDbServerQueue();
        for (String hostname : dbServerQueue) {
            assertTrue(utilisationMap.containsKey(hostname));
        }
        // Now let's simulate removing a host from DNS
        allConnections.remove("host-1.domain.de");
        CUT = new PoolImpl(allConnections, hazelcast);
        dbServerQueue = CUT.getDbServerQueue();
        for (String hostname : dbServerQueue) {
            assertNotNull(allConnections.get(hostname));
        }
    }

    public class PoolImpl extends Pool {

        public PoolImpl(Map<String, Object> allConnections, HazelcastInstance hazelcast) {
            super(allConnections, hazelcast);
        }
    }
    
}
