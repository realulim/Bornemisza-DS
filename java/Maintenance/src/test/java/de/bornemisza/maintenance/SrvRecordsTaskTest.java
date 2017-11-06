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

public class SrvRecordsTaskTest {

    private SecureRandom wheel;
    private IMap<String, Integer> utilisationMap;
    private List<String> dnsHostnames;
    
    public SrvRecordsTaskTest() {
    }
    
    @Before
    public void setUp() {
        this.wheel = new SecureRandom();
        utilisationMap = new PseudoHazelcastMap<>();
        dnsHostnames = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            utilisationMap.put(getHostname(i), wheel.nextInt(100) + 1);
            dnsHostnames.add(getHostname(i));
        }
        assertEquals(dnsHostnames.size(), utilisationMap.size());
    }

    @Test
    public void hostAppeared() {
        SrvRecordsTask CUT = new SrvRecordsTask(utilisationMap);

        Set<String> utilisedHostnames = new HashSet(utilisationMap.keySet());
        dnsHostnames.add(getHostname(999)); // simulated new SRV-Record
        CUT.updateDbServerUtilisation(utilisedHostnames, dnsHostnames);
        assertNotNull(utilisationMap.get(getHostname(999)));
        assertEquals(dnsHostnames.size(), utilisationMap.size());
    }

    @Test
    public void hostDisappeared() {
        SrvRecordsTask CUT = new SrvRecordsTask(utilisationMap);

        Set<String> utilisedHostnames = new HashSet(utilisationMap.keySet());
        dnsHostnames.remove(getHostname(3)); // simulate deleted SRV-Record
        CUT.updateDbServerUtilisation(utilisedHostnames, dnsHostnames);
        assertNull(utilisationMap.get(getHostname(3)));
        assertEquals(dnsHostnames.size(), utilisationMap.size());
    }

    private String getHostname(int i) {
        return "host" + i + ".mydatabase.com";
    }

}
