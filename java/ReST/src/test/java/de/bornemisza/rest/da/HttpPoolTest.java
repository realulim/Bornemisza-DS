package de.bornemisza.rest.da;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.loadbalancer.strategy.LoadBalancerStrategy;
import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.PseudoHazelcastMap;
import static de.bornemisza.loadbalancer.Config.*;

public class HttpPoolTest {

    class TestableHttpPool extends HttpPool {

        public TestableHttpPool(HazelcastInstance hz, LoadBalancerStrategy strategy, DnsProvider dnsProvider) {
            super(hz, strategy, dnsProvider, "someServiceName");
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
    private LoadBalancerStrategy strategy;
    private Map<String, HttpConnection> allTestConnections;
    private IMap dbServerUtilisation;
    private Get get;

    @Before
    public void setUp() {
        hazelcast = mock(HazelcastInstance.class);
        allTestConnections = new HashMap<>();
        Http http = mock(Http.class);
        HttpConnection conn = mock(HttpConnection.class);
        when(conn.getHttp()).thenReturn(http);
        get = mock(Get.class);
        when(http.get(anyString(), anyInt(), anyInt())).thenReturn(get);
        allTestConnections.put("host1", conn);
        allTestConnections.put("host2", conn);
        allTestConnections.put("host3", conn);

        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(new HashSet<>());
        when(hazelcast.getCluster()).thenReturn(cluster);
        dbServerUtilisation = new PseudoHazelcastMap();
        when(hazelcast.getMap(UTILISATION)).thenReturn(dbServerUtilisation);

        ITopic clusterMaintenanceTopic = mock(ITopic.class);
        when(hazelcast.getReliableTopic(anyString())).thenReturn(clusterMaintenanceTopic);

        strategy = mock(LoadBalancerStrategy.class);
        CUT = new TestableHttpPool(hazelcast, strategy, mock(DnsProvider.class));
    }

    @Test
    public void getConnection_noBackendsHealthy() {
        dbServerUtilisation.clear();
        when(get.responseCode()).thenReturn(404).thenThrow(new HttpException("")).thenReturn(200);
        HttpConnection conn = CUT.getConnection();
        assertEquals(allTestConnections.values().iterator().next(), conn);
    }

    @Test
    public void getConnection_noBackendsAvailableAtAll() {
        dbServerUtilisation.clear();
        when(get.responseCode()).thenReturn(404);
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
        List<String> dbServers = Arrays.asList(new String[] { "host4" });
        when(strategy.getHostQueue()).thenReturn(dbServers);

        int previousSize = allTestConnections.size();

        CUT.getConnection();

        assertEquals(previousSize + 1, allTestConnections.size()); // new connection was added
    }

    @Test
    public void getConnection_trackUsage() {
        List<String> dbServers = Arrays.asList(new String[] { "host1", "host2", "host3" });
        when(strategy.getHostQueue()).thenReturn(dbServers);
        int expectedUsageCount = 10;
        for (int i = 0; i < expectedUsageCount; i++) {
            assertNotNull(CUT.getConnection());
        }
        verify(strategy, times(expectedUsageCount)).trackUsage("host1");
    }

}
