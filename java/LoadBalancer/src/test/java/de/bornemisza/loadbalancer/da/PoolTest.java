package de.bornemisza.loadbalancer.da;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.OperationTimeoutException;

import de.bornemisza.loadbalancer.entity.PseudoHazelcastMap;

public class PoolTest {

    private HashMap<String, Object> allConnections;
    private HazelcastInstance hazelcast;
    private IList hazelcastList;
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
        this.hazelcastList = mock(IList.class);
        this.hazelcastMap = mock(IMap.class);
    }

    @Test
    public void hazelcastWorking() {
        when(hazelcastList.isEmpty()).thenReturn(true);
        when(hazelcastMap.isEmpty()).thenReturn(true);
        when(hazelcast.getList(anyString())).thenReturn(hazelcastList);
        when(hazelcast.getMap(anyString())).thenReturn(hazelcastMap);
        Pool CUT = new PoolImpl(allConnections, hazelcast);
        List<String> couchDbHostQueue = CUT.getCouchDbHostQueue();
        assertEquals(hazelcastList, couchDbHostQueue);
        verify(couchDbHostQueue).isEmpty();
        verify(couchDbHostQueue).addAll(allConnections.keySet());
        Map<String, Object> couchDbHostUtilisation = CUT.getCouchDbHostUtilisation();
        assertEquals(hazelcastMap, couchDbHostUtilisation);
        for (String hostname : allConnections.keySet()) {
            verify(couchDbHostUtilisation).isEmpty();
            verify(couchDbHostUtilisation).containsKey(hostname);
            verify(couchDbHostUtilisation).put(hostname, 0);
        }

        // subsequent invocations should just return the created data structures
        assertEquals(couchDbHostQueue, CUT.getCouchDbHostQueue());
        verifyNoMoreInteractions(couchDbHostQueue);
        assertEquals(couchDbHostUtilisation, CUT.getCouchDbHostUtilisation());
        verifyNoMoreInteractions(couchDbHostUtilisation);
    }

    @Test
    public void verifyRequestCounting() {
        when(hazelcast.getList(anyString())).thenReturn(mock(IList.class));
        when(hazelcast.getMap(anyString())).thenReturn(new PseudoHazelcastMap<>());
        Pool CUT = new PoolImpl(allConnections, hazelcast);
        Set<String> allHostnames = CUT.getAllHostnames();
        int requestCount = wheel.nextInt(15) + 1;
        for (String hostname : allHostnames) {
            for (int i = 0; i < requestCount; i++) {
                CUT.incrementRequestsFor(hostname);
            }
        }
        for (String hostname : allHostnames) {
            assertEquals(requestCount, CUT.getCouchDbHostUtilisation().get(hostname));
        }
        
    }

    @Test
    public void hazelcastNotWorkingAtFirst_thenWorkingLater() {
        String errMsg = "Invocation failed to complete due to operation-heartbeat-timeout";
        when(hazelcast.getList(anyString()))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenReturn(hazelcastList);
        when(hazelcast.getMap(anyString()))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenThrow(new OperationTimeoutException(errMsg))
                .thenReturn(hazelcastMap);
        Pool CUT = new PoolImpl(allConnections, hazelcast);
        List<String> couchDbHostQueue = CUT.getCouchDbHostQueue();
        assertNotEquals(hazelcastList, couchDbHostQueue);
        Map<String, Object> couchDbHostUtilisation = CUT.getCouchDbHostUtilisation();
        assertNotEquals(hazelcastMap, couchDbHostUtilisation);

        // subsequent invocation should return the Hazelcast data structure
        assertEquals(hazelcastList.hashCode(), CUT.getCouchDbHostQueue().hashCode());
        assertEquals(hazelcastMap.hashCode(), CUT.getCouchDbHostUtilisation().hashCode());
    }

    public class PoolImpl extends Pool {

        public PoolImpl(Map<String, Object> allConnections, HazelcastInstance hazelcast) {
            super(allConnections, hazelcast);
        }
    }
    
}
