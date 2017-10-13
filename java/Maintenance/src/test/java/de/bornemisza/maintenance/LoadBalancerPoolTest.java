package de.bornemisza.maintenance;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;

import de.bornemisza.loadbalancer.Config;

public class LoadBalancerPoolTest {

    private SecureRandom wheel;
    private HazelcastInstance hazelcast;

    public LoadBalancerPoolTest() {
    }

    @Before
    public void setUp() {
        this.wheel = new SecureRandom();
        hazelcast = mock(HazelcastInstance.class);
        IList dbServers = mock(IList.class);
        when(hazelcast.getList(Config.SERVERS)).thenReturn(dbServers);
    }

    @Test
    public void calculateMinuteExpression() {
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
        LoadBalancerPool CUT = new LoadBalancerPool(hazelcast, null);
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
        LoadBalancerPool CUT = new LoadBalancerPool(null, utilisationMap);
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
        LoadBalancerPool CUT = new LoadBalancerPool(null, utilisationMap);
        List<String> sortedHostnames = CUT.sortHostnamesByUtilisation();
        allHostnames.add(getHostname(10));
        allHostnames.add(getHostname(11));
        CUT.updateDbServerUtilisation(sortedHostnames, allHostnames);
        assertEquals(new Integer(0), utilisationMap.get(getHostname(10)));
        assertEquals(new Integer(0), utilisationMap.get(getHostname(11)));
        assertEquals(sortedHostnames.size() + 2, utilisationMap.size());

        allHostnames.remove(getHostname(3)); // simulate deleted SRV-Record
        CUT.updateDbServerUtilisation(sortedHostnames, allHostnames);
        assertEquals(0, (int)utilisationMap.get(getHostname(3)));
        assertEquals(sortedHostnames.size() + 2, utilisationMap.size());
    }

    private String getHostname(int i) {
        return "host" + i + ".mydatabase.com";
    }

}
