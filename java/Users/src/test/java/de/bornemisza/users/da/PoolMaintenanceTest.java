package de.bornemisza.users.da;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import de.bornemisza.loadbalancer.da.HealthChecks;

public class PoolMaintenanceTest {

    private SecureRandom wheel;
    private HealthChecks healthChecks;

    public PoolMaintenanceTest() {
    }

    @Before
    public void setUp() {
        this.wheel = new SecureRandom();
        this.healthChecks = mock(HealthChecks.class);
    }

    @Test
    public void calculateMinuteExpression() {
        HazelcastInstance hazelcast = mock(HazelcastInstance.class);
        Cluster cluster = mock(Cluster.class);
        when(hazelcast.getCluster()).thenReturn(cluster);
        Set<Member> members = new HashSet<>();
        Member member = null;
        for (int i = 0; i <= wheel.nextInt(10); i++) {
            member = mock(Member.class);
            when(member.getUuid()).thenReturn(UUID.randomUUID().toString());
            members.add(member);
        }
        when(cluster.getLocalMember()).thenReturn(member);
        when(cluster.getMembers()).thenReturn(members);
        PoolMaintenance CUT = new PoolMaintenance(hazelcast);
        String expr = CUT.calculateMinuteExpression();
        assertTrue(Character.getNumericValue(expr.charAt(0)) < members.size());
        assertEquals("/", String.valueOf(expr.charAt(1)));
        assertEquals(members.size(), Character.getNumericValue(expr.charAt(2)));
    }

    @Test
    public void sortHostnamesByUtilisation() throws Exception {
        Map<String, Integer> utilisationMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String randomKey = UUID.randomUUID().toString();
            utilisationMap.put(randomKey, wheel.nextInt(100));
        }
        PoolMaintenance CUT = new PoolMaintenance(utilisationMap);
        List<String> sortedKeys = CUT.sortHostnamesByUtilisation();
        int lastUtilisation = 0;
        for (String key : sortedKeys) {
            int utilisation = utilisationMap.get(key);
            assertTrue(lastUtilisation <= utilisation);
        }
    }

    @Test
    public void updateQueue_allHostsAvailable_filledQueue() {
        List<String> couchDbHostQueue = new ArrayList<>();
        couchDbHostQueue.add("hostname2");
        couchDbHostQueue.add("hostname3");
        couchDbHostQueue.add("hostname1");
        when(healthChecks.isHostAvailable(anyString(), anyInt())).thenReturn(true);
        PoolMaintenance CUT = new PoolMaintenance(couchDbHostQueue, healthChecks);
        List<String> sortedHostnames = new ArrayList<>();
        sortedHostnames.add("hostname1");
        sortedHostnames.add("hostname2");
        sortedHostnames.add("hostname3");
        CUT.updateQueue(sortedHostnames);
        assertEquals(sortedHostnames, couchDbHostQueue);
    }

    @Test
    public void updateQueue_emptyQueue() {
        List<String> couchDbHostQueue = new ArrayList<>();
        when(healthChecks.isHostAvailable(anyString(), anyInt())).thenReturn(true);
        PoolMaintenance CUT = new PoolMaintenance(couchDbHostQueue, healthChecks);
        List<String> sortedHostnames = new ArrayList<>();
        sortedHostnames.add("hostname1");
        sortedHostnames.add("hostname2");
        sortedHostnames.add("hostname3");
        CUT.updateQueue(sortedHostnames);
        assertEquals(sortedHostnames, couchDbHostQueue);
    }

    @Test
    public void updateQueue_someHostsNotAvailable() {
        List<String> couchDbHostQueue = new ArrayList<>();
        couchDbHostQueue.add("hostname2");
        couchDbHostQueue.add("hostname4");
        couchDbHostQueue.add("hostname1");
        couchDbHostQueue.add("hostname3");
        when(healthChecks.isHostAvailable(anyString(), anyInt())).thenReturn(true).thenReturn(false).thenReturn(false).thenReturn(true);
        PoolMaintenance CUT = new PoolMaintenance(couchDbHostQueue, healthChecks);
        List<String> sortedHostnames = new ArrayList<>();
        sortedHostnames.add("hostname1");
        sortedHostnames.add("hostname2");
        sortedHostnames.add("hostname3");
        sortedHostnames.add("hostname4");
        CUT.updateQueue(sortedHostnames);
        assertEquals("hostname1", couchDbHostQueue.get(0));
        assertEquals("hostname4", couchDbHostQueue.get(1));
        assertEquals(2, couchDbHostQueue.size());
    }

}
