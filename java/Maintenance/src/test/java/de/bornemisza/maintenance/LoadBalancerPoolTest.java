package de.bornemisza.maintenance;

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

public class LoadBalancerPoolTest {

    private SecureRandom wheel;

    public LoadBalancerPoolTest() {
    }

    @Before
    public void setUp() {
        this.wheel = new SecureRandom();
    }

    //@Test
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
        LoadBalancerPool CUT = new LoadBalancerPool(hazelcast);
        String expr = CUT.calculateMinuteExpression();
        assertTrue(Character.getNumericValue(expr.charAt(0)) < members.size());
        assertEquals("/", String.valueOf(expr.charAt(1)));
        assertEquals(members.size(), Character.getNumericValue(expr.charAt(2)));
    }

    //@Test
    public void sortHostnamesByUtilisation() throws Exception {
        Map<String, Integer> utilisationMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String randomKey = UUID.randomUUID().toString();
            utilisationMap.put(randomKey, wheel.nextInt(100));
        }
        LoadBalancerPool CUT = new LoadBalancerPool(utilisationMap);
        List<String> sortedKeys = CUT.sortHostnamesByUtilisation();
        int lastUtilisation = 0;
        for (String key : sortedKeys) {
            int utilisation = utilisationMap.get(key);
            assertTrue(lastUtilisation <= utilisation);
        }
    }

    @Test
    public void addNewHostsForService() {
        Map<String, Integer> utilisationMap = new HashMap<>();
        List<String> allHostnames = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            utilisationMap.put(getHostname(i), wheel.nextInt(100) + 1);
            allHostnames.add(getHostname(i));
        }
        LoadBalancerPool CUT = new LoadBalancerPool(utilisationMap);
        List<String> sortedHostnames = CUT.sortHostnamesByUtilisation();
        allHostnames.add(getHostname(10));
        allHostnames.add(getHostname(11));
        CUT.updateHostList(sortedHostnames, allHostnames);
        assertEquals(getHostname(11), sortedHostnames.get(0));
        assertEquals(getHostname(10), sortedHostnames.get(1));
        assertEquals(new Integer(0), utilisationMap.get(getHostname(10)));
        assertEquals(new Integer(0), utilisationMap.get(getHostname(11)));
        assertEquals(sortedHostnames.size(), utilisationMap.size());

        allHostnames.remove(getHostname(3)); // simulate deleted SRV-Record
        CUT.updateHostList(sortedHostnames, allHostnames);
        assertFalse(sortedHostnames.contains(getHostname(3)));
        assertEquals(allHostnames.size(), sortedHostnames.size());
        assertNull(utilisationMap.get(getHostname(3)));
        assertEquals(sortedHostnames.size(), utilisationMap.size());
    }

    private String getHostname(int i) {
        return "host" + i + ".mydatabase.com";
    }

    //@Test
    public void updateQueue_emptyQueue() {
        List<String> dbServers = new ArrayList<>();
        LoadBalancerPool CUT = new LoadBalancerPool(dbServers);
        List<String> sortedHostnames = new ArrayList<>();
        sortedHostnames.add("hostname1");
        sortedHostnames.add("hostname2");
        sortedHostnames.add("hostname3");
        CUT.updateQueue(sortedHostnames);
        assertEquals(sortedHostnames, dbServers);
    }

    //@Test
    public void updateQueue_filledQueue() {
        List<String> dbServers = new ArrayList<>();
        dbServers.add("hostname2");
        dbServers.add("hostname3");
        dbServers.add("hostname1");
        LoadBalancerPool CUT = new LoadBalancerPool(dbServers);
        List<String> sortedHostnames = new ArrayList<>();
        sortedHostnames.add("hostname1");
        sortedHostnames.add("hostname2");
        sortedHostnames.add("hostname3");
        CUT.updateQueue(sortedHostnames);
        assertEquals(sortedHostnames, dbServers);
    }
}
