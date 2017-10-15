package de.bornemisza.maintenance;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.hazelcast.core.IMap;

import de.bornemisza.maintenance.entity.PseudoHazelcastMap;

public class LoadBalancerTaskTest {

    private SecureRandom wheel;
    
    public LoadBalancerTaskTest() {
    }
    
    @Before
    public void setUp() {
        this.wheel = new SecureRandom();
    }

    @Test
    public void addNewHostsForService() {
        IMap<String, Integer> utilisationMap = new PseudoHazelcastMap<>();
        List<String> dnsHostnames = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            utilisationMap.put(getHostname(i), wheel.nextInt(100) + 1);
            dnsHostnames.add(getHostname(i));
        }
        LoadBalancerTask CUT = new LoadBalancerTask(utilisationMap);
        assertEquals(dnsHostnames.size(), utilisationMap.size());

        dnsHostnames.add(getHostname(10)); // simulate added SRV-Records
        dnsHostnames.add(getHostname(11));
        Set<String> utilisedHostnames = new HashSet(utilisationMap.keySet());
        CUT.updateDbServerUtilisation(utilisedHostnames, dnsHostnames);
        assertEquals(dnsHostnames.size(), utilisationMap.size());
        for (String hostname : utilisationMap.keySet()) {
            assertEquals(new Integer(0), utilisationMap.get(hostname));
        }

        utilisedHostnames = new HashSet(utilisationMap.keySet());
        dnsHostnames.remove(getHostname(3)); // simulate deleted SRV-Record
        CUT.updateDbServerUtilisation(utilisedHostnames, dnsHostnames);
        assertNull(utilisationMap.get(getHostname(3)));
        assertEquals(dnsHostnames.size(), utilisationMap.size());
    }

    private String getHostname(int i) {
        return "host" + i + ".mydatabase.com";
    }

}
