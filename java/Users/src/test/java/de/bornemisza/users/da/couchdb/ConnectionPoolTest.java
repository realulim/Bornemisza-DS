package de.bornemisza.users.da.couchdb;

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

import de.bornemisza.users.HealthChecks;
import de.bornemisza.users.PseudoHazelcastList;
import de.bornemisza.users.PseudoHazelcastMap;
import de.bornemisza.users.boundary.BasicAuthCredentials;
import de.bornemisza.users.da.CouchDbConnection;

public class ConnectionPoolTest {

    private final SecureRandom wheel = new SecureRandom();

    private List<String> hostnames;
    private Map<String, CouchDbConnection> allConnections;
    private HazelcastInstance hazelcast;
    private PseudoHazelcastList hostQueue;
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
        hostQueue = new PseudoHazelcastList();
        when(hazelcast.getList(anyString())).thenReturn(hostQueue);
        utilisationMap = new PseudoHazelcastMap();
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        
        healthChecks = mock(HealthChecks.class);

        CUT = new ConnectionPool(allConnections, hazelcast, healthChecks);
    }

    @Test
    public void getMember_emptyHostQueue_noUtilisation_allAvailable() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        CouchDbConnector dbConn = CUT.getConnection();
        assertNotNull(dbConn);
        assertEquals(allConnections.size(), hostQueue.size(), utilisationMap.size());
    }

    @Test
    public void getMember_emptyHostQueue_noUtilisation_notAllAvailable() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        CouchDbConnector dbConn = CUT.getConnection();
        assertEquals(allConnections.size(), hostQueue.size(), utilisationMap.size() - 1);
        if (allConnections.size() > 1) assertNotNull(dbConn); // we need at least two hosts, because the first is unavailable
    }

    @Test
    public void getMember_emptyHostQueue_noUtilisation_noneAvailable() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(false);
        try {
            CUT.getConnection();
            fail();
        }
        catch (DbAccessException ex) {
            // expected
            assertEquals(0, hostQueue.size(), utilisationMap.size());
        }
    }

    @Test
    public void getMember_preExisting_HostQueue_and_Utilisation() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        String hostname = "hostname.domain.de";
        allConnections.clear();
        allConnections.put(hostname, getConnection());
        hostQueue.clear();
        hostQueue.add(hostname);
        utilisationMap.clear();
        int startUsageCount = wheel.nextInt(1000);
        utilisationMap.put(hostname, startUsageCount);
        int additionalUsageCount = wheel.nextInt(10);
        for (int i = 0; i < additionalUsageCount; i++) {
            assertNotNull(CUT.getConnection());
        }
        assertEquals(startUsageCount + additionalUsageCount, utilisationMap.get(hostname));
    }

    @Test
    public void getMember_nullCredentials() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        CouchDbConnection conn = getConnectionMock();
        String hostname = "hostname.domain.de";
        allConnections.clear();
        allConnections.put(hostname, conn);
        hostQueue.clear();
        hostQueue.add(hostname);
        utilisationMap.clear();
        utilisationMap.put(hostname, 1);
 
        CUT.getConnection(null);
        verify(conn).getUrl();
        verify(conn).getDatabaseName();
        verify(conn).getUserName();
        verify(conn).getPassword();
    }

    @Test
    public void getMember_credentialsGiven() {
        when(healthChecks.isCouchDbReady(any(CouchDbConnection.class))).thenReturn(true);
        CouchDbConnection conn = getConnectionMock();
        String hostname = "hostname.domain.de";
        allConnections.clear();
        allConnections.put(hostname, conn);
        hostQueue.clear();
        hostQueue.add(hostname);
        utilisationMap.clear();
        utilisationMap.put(hostname, 1);
 
        BasicAuthCredentials creds = getBasicAuthCredentialsMock();
        CUT.getConnection(creds);
        verify(conn).getUrl();
        verify(conn).getDatabaseName();
        verify(creds).getUserName();
        verify(creds).getPassword();
        verifyNoMoreInteractions(conn);
        verifyNoMoreInteractions(creds);
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
            when(conn.getUrl()).thenReturn(new URL("https://localhost/"));
            when(conn.getUserName()).thenReturn("admin");
            when(conn.getPassword()).thenReturn("secret");
            when(conn.getDatabaseName()).thenReturn("users");
            return conn;
        } 
        catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private BasicAuthCredentials getBasicAuthCredentialsMock() {
        BasicAuthCredentials creds = mock(BasicAuthCredentials.class);
        when(creds.getUserName()).thenReturn("admin");
        when(creds.getPassword()).thenReturn("secret");
        return creds;
    }

}
