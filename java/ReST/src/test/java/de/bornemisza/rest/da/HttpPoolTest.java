package de.bornemisza.rest.da;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import static de.bornemisza.loadbalancer.Config.*;
import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;
import de.bornemisza.rest.PseudoHazelcastMap;

public class HttpPoolTest {

    class TestableHttpPool extends HttpPool {

        public TestableHttpPool(HazelcastInstance hz, DnsProvider dnsProvider, HealthChecks healthChecks) {
            super(hz, dnsProvider, healthChecks, "someServiceName");
        }

        // expose protected method for testing
        public List<String> getDbServers() {
            return getDbServerQueue();
        }

        @Override
        protected LoadBalancerConfig getLoadBalancerConfig() {
            return null; // nothing
        }

        @Override
        protected Map<String, Http> createConnections() {
            return allTestConnections;
        }

    }

    private TestableHttpPool CUT;
    private HazelcastInstance hazelcast;
    private HealthChecks healthChecks;
    private Map<String, Http> allTestConnections;
    private IMap dbServerUtilisation;

    @Before
    public void setUp() {
        hazelcast = mock(HazelcastInstance.class);
        healthChecks = mock(HealthChecks.class);
        allTestConnections = new HashMap<>();
        allTestConnections.put("host1", mock(Http.class));
        allTestConnections.put("host2", mock(Http.class));
        allTestConnections.put("host3", mock(Http.class));

        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(new HashSet<>());
        when(hazelcast.getCluster()).thenReturn(cluster);
        dbServerUtilisation = new PseudoHazelcastMap();
        when(hazelcast.getMap(UTILISATION)).thenReturn(dbServerUtilisation);

        CUT = new TestableHttpPool(hazelcast, mock(DnsProvider.class), healthChecks);
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
            verify(healthChecks, times(allTestConnections.keySet().size())).isCouchDbReady(any(Http.class));
        }
    }

    @Test
    public void getConnection_someHealthChecksFailed() {
        when(healthChecks.isCouchDbReady(any())).thenReturn(false).thenReturn(true);
        assertNotNull(CUT.getConnection());
        int usageCount = (int) dbServerUtilisation.get("host1");
        usageCount += (int) dbServerUtilisation.get("host2");
        usageCount += (int) dbServerUtilisation.get("host3");
        assertEquals(1, usageCount);
        verify(healthChecks, times(allTestConnections.keySet().size() - 1)).isCouchDbReady(any(Http.class));
        assertEquals(3, CUT.getDbServers().size());
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
