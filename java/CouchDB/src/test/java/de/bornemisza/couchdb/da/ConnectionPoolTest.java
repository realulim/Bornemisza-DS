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

import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;

import de.bornemisza.couchdb.HealthChecks;
import de.bornemisza.couchdb.PseudoHazelcastList;
import de.bornemisza.couchdb.PseudoHazelcastMap;
import de.bornemisza.couchdb.entity.CouchDbConnection;

public class ConnectionPoolTest {

    private final SecureRandom wheel = new SecureRandom();

    private List<String> hostnames;
    private Map<String, CouchDbConnection> allConnections;
    private HazelcastInstance hazelcast;
    private PseudoHazelcastList dbServerQueue;
    private PseudoHazelcastMap utilisationMap;
    private HealthChecks healthChecks;

    private ConnectionPool CUT;

    @Before
    public void setUp() {
        hostnames = new ArrayList<>();
        for (int i = 0; i <= wheel.nextInt(9); i++) {
            hostnames.add("hostname" + i + ".domain.de");
        }
        
        allConnections = new HashMap<>();
        CouchDbConnection conn = getConnection();
        for (String hostname : hostnames) {
            allConnections.put(hostname, conn);
        }
        
        hazelcast = mock(HazelcastInstance.class);
        dbServerQueue = new PseudoHazelcastList();
        when(hazelcast.getList(anyString())).thenReturn(dbServerQueue);
        utilisationMap = new PseudoHazelcastMap();
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        
        healthChecks = mock(HealthChecks.class);

        CUT = new ConnectionPool(allConnections, hazelcast, healthChecks);
    }

    @Test
    public void getConnector_emptyHostQueue_noUtilisation_allAvailable() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        CouchDbConnector dbConn = CUT.getConnector();
        assertNotNull(dbConn);
        assertEquals(allConnections.size(), dbServerQueue.size(), utilisationMap.size());
    }

    @Test
    public void getConnector_emptyHostQueue_noUtilisation_notAllAvailable() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        CouchDbConnector dbConn = CUT.getConnector();
        assertEquals(allConnections.size(), dbServerQueue.size(), utilisationMap.size() - 1);
        if (allConnections.size() > 1) assertNotNull(dbConn); // we need at least two hosts, because the first is unavailable
    }

    @Test
    public void getConnector_emptyHostQueue_noUtilisation_noneAvailable() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(false);
        try {
            CUT.getConnector();
            fail();
        }
        catch (DbAccessException ex) {
            // expected
            assertEquals(0, dbServerQueue.size(), utilisationMap.size());
        }
    }

    @Test
    public void getConnector_preExisting_HostQueue_and_Utilisation() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        String hostname = "hostname.domain.de";
        allConnections.clear();
        allConnections.put(hostname, getConnection());
        dbServerQueue.clear();
        dbServerQueue.add(hostname);
        utilisationMap.clear();
        int startUsageCount = wheel.nextInt(1000);
        utilisationMap.put(hostname, startUsageCount);
        int additionalUsageCount = wheel.nextInt(10);
        for (int i = 0; i < additionalUsageCount; i++) {
            assertNotNull(CUT.getConnector());
        }
        assertEquals(startUsageCount + additionalUsageCount, utilisationMap.get(hostname));
    }

    @Test
    public void getConnector_hostQueueWithUnhealthyServers() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(false).thenReturn(true);
        String hostname1 = "hostname1.domain.de";
        String hostname2 = "hostname2.domain.de";
        allConnections.clear();
        allConnections.put(hostname1, getConnection());
        allConnections.put(hostname2, getConnection());
        dbServerQueue.clear();
        dbServerQueue.add(hostname1);
        dbServerQueue.add(hostname2);
        utilisationMap.clear();
        int startUsageCount = wheel.nextInt(1000);
        utilisationMap.put(hostname1, 0);
        utilisationMap.put(hostname2, startUsageCount);
        int additionalUsageCount = wheel.nextInt(10);
        for (int i = 0; i < additionalUsageCount; i++) {
            assertNotNull(CUT.getConnector());
        }
        assertEquals(startUsageCount + additionalUsageCount, utilisationMap.get(hostname2));
        assertEquals(dbServerQueue.get(0), dbServerQueue.get(2));
        assertEquals(3, dbServerQueue.size());
    }

    @Test
    public void getConnector_nullCredentials() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        CouchDbConnection conn = getConnectionMock();
        String hostname = "hostname.domain.de";
        allConnections.clear();
        allConnections.put(hostname, conn);
        dbServerQueue.clear();
        dbServerQueue.add(hostname);
        utilisationMap.clear();
        utilisationMap.put(hostname, 1);
 
        CUT.getConnector(null, null);
        verify(conn).getBaseUrl();
        verify(conn).getDatabaseName();
        verify(conn).getUserName();
        verify(conn).getPassword();
    }

    @Test
    public void getConnector_credentialsGiven() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        CouchDbConnection conn = getConnectionMock();
        String hostname = "hostname.domain.de";
        allConnections.clear();
        allConnections.put(hostname, conn);
        dbServerQueue.clear();
        dbServerQueue.add(hostname);
        utilisationMap.clear();
        utilisationMap.put(hostname, 1);
 
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
