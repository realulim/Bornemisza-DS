package de.bornemisza.maintenance;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.mock;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import de.bornemisza.maintenance.entity.PseudoHazelcastMap;

public class LoadBalancerTaskTest {

    private SecureRandom wheel;
    private HazelcastInstance hazelcast;
    
    public LoadBalancerTaskTest() {
    }
    
    @Before
    public void setUp() {
        this.wheel = new SecureRandom();
        hazelcast = mock(HazelcastInstance.class);
    }

    @Test
    public void addNewHostsForService() {
        IMap<String, Integer> utilisationMap = new PseudoHazelcastMap<>();
        List<String> allHostnames = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            utilisationMap.put(getHostname(i), wheel.nextInt(100) + 1);
            allHostnames.add(getHostname(i));
        }
        LoadBalancerTask CUT = new LoadBalancerTask(hazelcast, utilisationMap);
        allHostnames.add(getHostname(10));
        allHostnames.add(getHostname(11));
        Set<String> utilisedHostnames = new HashSet<>(utilisationMap.keySet());
        CUT.updateDbServerUtilisation(utilisedHostnames, allHostnames);
        for (String hostname : utilisationMap.keySet()) {
            assertEquals(new Integer(0), utilisationMap.get(hostname));
        }
        assertEquals(allHostnames.size(), utilisationMap.size());

        allHostnames.remove(getHostname(3)); // simulate deleted SRV-Record
        CUT.updateDbServerUtilisation(utilisedHostnames, allHostnames);
        assertNull(utilisationMap.get(getHostname(3)));
        assertEquals(allHostnames.size(), utilisationMap.size());
    }

    private String getHostname(int i) {
        return "host" + i + ".mydatabase.com";
    }


}
