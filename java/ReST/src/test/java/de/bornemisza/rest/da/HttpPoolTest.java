package de.bornemisza.rest.da;

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
import com.hazelcast.core.ITopic;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.PseudoHazelcastMap;
import static de.bornemisza.loadbalancer.Config.*;

public class HttpPoolTest {

    class TestableHttpPool extends HttpPool {

        public TestableHttpPool(HazelcastInstance hz, DnsProvider dnsProvider) {
            super(hz, dnsProvider, "someServiceName");
        }

        @Override
        protected LoadBalancerConfig getLoadBalancerConfig() {
            return new LoadBalancerConfig("serviceName", "instanceName", "someUser", "password".toCharArray());
        }

        @Override
        protected Map<String, HttpConnection> createConnections() {
            return allTestConnections;
        }

        @Override
        protected List<String> getDbServerQueue() {
            return super.getDbServerQueue();
        }
    }

    private TestableHttpPool CUT;
    private HazelcastInstance hazelcast;
    private Map<String, HttpConnection> allTestConnections;
    private IMap dbServerUtilisation;

    @Before
    public void setUp() {
        hazelcast = mock(HazelcastInstance.class);
        allTestConnections = new HashMap<>();
        allTestConnections.put("host1", mock(HttpConnection.class));
        allTestConnections.put("host2", mock(HttpConnection.class));
        allTestConnections.put("host3", mock(HttpConnection.class));

        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(new HashSet<>());
        when(hazelcast.getCluster()).thenReturn(cluster);
        dbServerUtilisation = new PseudoHazelcastMap();
        when(hazelcast.getMap(UTILISATION)).thenReturn(dbServerUtilisation);

        ITopic clusterMaintenanceTopic = mock(ITopic.class);
        when(hazelcast.getReliableTopic(anyString())).thenReturn(clusterMaintenanceTopic);

        CUT = new TestableHttpPool(hazelcast, mock(DnsProvider.class));
    }

    @Test
    public void getConnection_noBackendsAvailable() {
        dbServerUtilisation.clear();
        try {
            CUT.getConnection();
            fail();
        }
        catch (IllegalStateException e) {
            assertEquals("No DbServer available at all!", e.getMessage());
        }
    }

    @Test
    public void getConnection_connectionNull() {
        dbServerUtilisation.put("host0", -1); // need to have fewer then 0 (which all other hosts start on)

        int previousSize = allTestConnections.size();

        CUT.getConnection();

        assertEquals(previousSize + 1, allTestConnections.size()); // new connection was added
    }

    @Test
    public void getConnection_usageCountIncrementer() {
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
