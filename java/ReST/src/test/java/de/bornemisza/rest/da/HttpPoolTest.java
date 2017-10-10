package de.bornemisza.rest.da;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;

import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;
import de.bornemisza.rest.PseudoHazelcastList;
import de.bornemisza.rest.PseudoHazelcastMap;

import static de.bornemisza.loadbalancer.Config.SERVERS;
import static de.bornemisza.loadbalancer.Config.UTILISATION;

public class HttpPoolTest {

    private HttpPool CUT;
    private HazelcastInstance hazelcast;
    private HealthChecks healthChecks;
    private Map<String, Http> allConnections;
    private IMap dbServerUtilisation;
    private IList dbServerQueue;

    @Before
    public void setUp() {
        hazelcast = mock(HazelcastInstance.class);
        healthChecks = mock(HealthChecks.class);
        allConnections = new HashMap<>();
        allConnections.put("host1", mock(Http.class));
        allConnections.put("host2", mock(Http.class));
        allConnections.put("host3", mock(Http.class));

        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(new HashSet<>());
        when(hazelcast.getCluster()).thenReturn(cluster);
        dbServerQueue = new PseudoHazelcastList();
        when(hazelcast.getList(SERVERS)).thenReturn(dbServerQueue);
        dbServerUtilisation = new PseudoHazelcastMap();
        when(hazelcast.getMap(UTILISATION)).thenReturn(dbServerUtilisation);
        CUT = new HttpPool(allConnections, hazelcast, healthChecks);
    }

    @Test
    public void getConnection_allHealthChecksFailed() {
        when(healthChecks.isCouchDbReady(any())).thenReturn(false);
        try {
            CUT.getConnection();
            fail();
        }
        catch (RuntimeException ex) {
            assertEquals("No CouchDB Backend ready!", ex.getMessage());
            verify(healthChecks, times(allConnections.keySet().size())).isCouchDbReady(any(Http.class));
        }
    }

    @Test
    public void getConnection_someHealthChecksFailed() {
        when(healthChecks.isCouchDbReady(any())).thenReturn(false).thenReturn(true);
        assertNotNull(CUT.getConnection());
        Integer usageCount = (Integer) dbServerUtilisation.get("host1");
        usageCount += (Integer) dbServerUtilisation.get("host2");
        usageCount += (Integer) dbServerUtilisation.get("host3");
        assertEquals(1, usageCount.intValue());
        verify(healthChecks, times(allConnections.keySet().size() - 1)).isCouchDbReady(any(Http.class));
        assertEquals(dbServerQueue.get(0), dbServerQueue.get(2));
        assertEquals(4, dbServerQueue.size());
    }

    @Test
    public void getConnection_usageCountIncrementer() {
        when(healthChecks.isCouchDbReady(any())).thenReturn(true);
        int expectedUsageCount = 10;
        for (int i = 0; i < expectedUsageCount; i++) {
            assertNotNull(CUT.getConnection());
        }
        Integer usageCount = (Integer) dbServerUtilisation.get("host1");
        usageCount += (Integer) dbServerUtilisation.get("host2");
        usageCount += (Integer) dbServerUtilisation.get("host3");
        assertEquals(expectedUsageCount, usageCount.intValue());
    }

}
