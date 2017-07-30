package de.bornemisza.users.da.couchdb;

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

import de.bornemisza.users.PseudoHazelcastList;
import de.bornemisza.users.PseudoHazelcastMap;

public class ConnectorPoolTest {

    private final SecureRandom wheel = new SecureRandom();

    private List<String> hostnames;
    private Map<String, CouchDbConnector> allConnectors;
    private HazelcastInstance hazelcast;
    private PseudoHazelcastList hostQueue;
    private PseudoHazelcastMap utilisationMap;
    private HealthChecks healthChecks;

    private ConnectorPool CUT;

    @Before
    public void setUp() {
        hostnames = new ArrayList<>();
        for (int i = 0; i <= wheel.nextInt(9); i++) {
            hostnames.add("hostname" + i + ".domain.de");
        }
        
        allConnectors = new HashMap<>();
        for (String hostname : hostnames) {
            allConnectors.put(hostname, mock(CouchDbConnector.class));
        }
        
        hazelcast = mock(HazelcastInstance.class);
        hostQueue = new PseudoHazelcastList();
        when(hazelcast.getList(anyString())).thenReturn(hostQueue);
        utilisationMap = new PseudoHazelcastMap();
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        
        healthChecks = mock(HealthChecks.class);

        CUT = new ConnectorPool(allConnectors, hazelcast, healthChecks);
    }

    @Test
    public void getMember_emptyHostQueue_noUtilisation_allAvailable() {
        when(healthChecks.isHostAvailable(anyString(), anyInt())).thenReturn(true);
        CouchDbConnector db = CUT.getMember();
        assertNotNull(db);
        assertEquals(allConnectors.size(), hostQueue.size(), utilisationMap.size());
    }

    @Test
    public void getMember_emptyHostQueue_noUtilisation_notAllAvailable() {
        when(healthChecks.isHostAvailable(anyString(), anyInt())).thenReturn(false).thenReturn(true);
        CouchDbConnector db = CUT.getMember();
        assertEquals(allConnectors.size(), hostQueue.size(), utilisationMap.size() - 1);
        if (allConnectors.size() > 1) assertNotNull(db); // we need at least two hosts, because the first is unavailable
    }

    @Test
    public void getMember_emptyHostQueue_noUtilisation_noneAvailable() {
        when(healthChecks.isHostAvailable(anyString(), anyInt())).thenReturn(false);
        CouchDbConnector db = CUT.getMember();
        assertEquals(0, hostQueue.size(), utilisationMap.size());
        assertNull(db);
    }

    @Test
    public void getMember_preExisting_HostQueue_and_Utilisation() {
        when(healthChecks.isHostAvailable(anyString(), anyInt())).thenReturn(true);
        String hostname = "hostname.domain.de";
        allConnectors.clear();
        allConnectors.put(hostname, mock(CouchDbConnector.class));
        hostQueue.clear();
        hostQueue.add(hostname);
        utilisationMap.clear();
        int startUsageCount = wheel.nextInt(1000);
        utilisationMap.put(hostname, startUsageCount);
        int additionalUsageCount = wheel.nextInt(10);
        for (int i = 0; i < additionalUsageCount; i++) {
            assertNotNull(CUT.getMember());
        }
        assertEquals(startUsageCount + additionalUsageCount, utilisationMap.get(hostname));
    }

}
