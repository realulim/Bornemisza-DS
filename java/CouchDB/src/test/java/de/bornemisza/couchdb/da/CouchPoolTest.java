package de.bornemisza.couchdb.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

import de.bornemisza.couchdb.PseudoHazelcastList;
import de.bornemisza.couchdb.PseudoHazelcastMap;
import de.bornemisza.couchdb.entity.CouchDbConnection;
import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;

public class CouchPoolTest {

    class TestableConnectionPool extends CouchPool {

        public TestableConnectionPool(HazelcastInstance hz, DnsProvider dnsProvider) {
            super(hz, dnsProvider, "someServiceName");
        }

        // expose protected method for testing
        public List<String> getDbServers() {
            return getDbServerQueue();
        }

        @Override
        protected LoadBalancerConfig getLoadBalancerConfig() {
            return new LoadBalancerConfig("serviceName", "instanceName", "someUser", "password".toCharArray());
        }

        @Override
        protected Map<String, CouchDbConnection> createConnections() {
            return allTestConnections;
        }

    }

    private final SecureRandom wheel = new SecureRandom();

    private List<String> hostnames;
    private Map<String, CouchDbConnection> allTestConnections;
    private PseudoHazelcastList dbServerQueue;
    private HazelcastInstance hazelcast;
    private PseudoHazelcastMap dbServerUtilisation;

    private TestableConnectionPool CUT;

    @Before
    public void setUp() {
        hostnames = new ArrayList<>();
        for (int i = 0; i <= wheel.nextInt(9); i++) {
            hostnames.add("hostname" + i + ".domain.de");
        }
        
        allTestConnections = new HashMap<>();
        CouchDbConnection conn = getConnection();
        for (String hostname : hostnames) {
            allTestConnections.put(hostname, conn);
        }
        
        hazelcast = mock(HazelcastInstance.class);
        dbServerQueue = new PseudoHazelcastList();
        when(hazelcast.getList(anyString())).thenReturn(dbServerQueue);
        dbServerUtilisation = new PseudoHazelcastMap();
        when(hazelcast.getMap(anyString())).thenReturn(dbServerUtilisation);

        ITopic clusterMaintenanceTopic = mock(ITopic.class);
        when(hazelcast.getReliableTopic(anyString())).thenReturn(clusterMaintenanceTopic);
        
        CUT = new TestableConnectionPool(hazelcast, mock(DnsProvider.class));
    }

    @Test
    public void getConnection_connectionNull() {
        dbServerUtilisation.put("host0", -1); // need to have fewer then 0 (which all other hosts start on)

        int previousSize = allTestConnections.size();

        CUT.getConnector("admin", "secret".toCharArray());

        assertEquals(previousSize + 1, allTestConnections.size()); // new connection was added
    }

    @Test
    public void getConnection_noBackendsAvailable() {
        dbServerUtilisation.clear();
        try {
        CUT.getConnector("admin", "secret".toCharArray());
            fail();
        }
        catch (IllegalStateException e) {
            assertEquals("No DbServer available at all!", e.getMessage());
        }
    }

    @Test
    public void getConnector() {
        String hostname = "hostname.domain.de";
        allTestConnections.clear();
        allTestConnections.put(hostname, getConnection());
        CUT.getDbServers().clear();
        CUT.getDbServers().add(hostname);
        dbServerUtilisation.clear();
        int startUsageCount = wheel.nextInt(1000);
        dbServerUtilisation.put(hostname, startUsageCount);
        int additionalUsageCount = wheel.nextInt(10);
        for (int i = 0; i < additionalUsageCount; i++) {
            assertNotNull(CUT.getConnector("admin", "secret".toCharArray()));
        }
        assertEquals(startUsageCount + additionalUsageCount, dbServerUtilisation.get(hostname));
    }

    @Test
    public void getConnector_credentialsGiven() {
        CouchDbConnection conn = getConnectionMock();
        String hostname = "hostname.domain.de";
        allTestConnections.clear();
        allTestConnections.put(hostname, conn);
        CUT.getDbServers().clear();
        CUT.getDbServers().add(hostname);
        dbServerUtilisation.clear();
        dbServerUtilisation.put(hostname, 1);
 
        String userName = "Ike";
        char[] password = new char[] {'p', 'w'};
        CUT.getConnector(userName, password);
        verify(conn).getBaseUrl();
        verify(conn).getDatabaseName();
        verifyNoMoreInteractions(conn);
    }

    private CouchDbConnection getConnection() {
        try {
            return new CouchDbConnection(new URL("https://localhost/"), "users", "admin", "secret");
        } 
        catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private CouchDbConnection getConnectionMock() {
        CouchDbConnection conn = mock(CouchDbConnection.class);
        try {
            when(conn.getBaseUrl()).thenReturn(new URL("https://localhost/"));
            when(conn.getUserName()).thenReturn("admin");
            when(conn.getPassword()).thenReturn("secret");
            when(conn.getDatabaseName()).thenReturn("users");
            return conn;
        } 
        catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

}
