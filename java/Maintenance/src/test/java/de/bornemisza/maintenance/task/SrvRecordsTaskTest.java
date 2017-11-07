package de.bornemisza.maintenance.task;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.ClusterEvent.ClusterEventType;
import de.bornemisza.maintenance.entity.PseudoHazelcastMap;

public class SrvRecordsTaskTest {

    private IMap utilisationMap;
    private List<String> dnsHostnames;
    private ITopic clusterMaintenanceTopic;
    private ArgumentCaptor<ClusterEvent> captor;

    public SrvRecordsTaskTest() {
    }

    @Before
    public void setUp() {
        SecureRandom wheel = new SecureRandom();
        utilisationMap = new PseudoHazelcastMap<>();
        dnsHostnames = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            utilisationMap.put(getHostname(i), wheel.nextInt(100) + 1);
            dnsHostnames.add(getHostname(i));
        }
        assertEquals(dnsHostnames.size(), utilisationMap.size());

        captor = ArgumentCaptor.forClass(ClusterEvent.class);

        HazelcastInstance hazelcast = mock(HazelcastInstance.class);
        clusterMaintenanceTopic = mock(ITopic.class);
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        when(hazelcast.getReliableTopic(anyString())).thenReturn(clusterMaintenanceTopic);
    }

    @Test
    public void hostAppeared() {
        SrvRecordsTask CUT = new SrvRecordsTask(utilisationMap, clusterMaintenanceTopic);

        Set<String> utilisedHostnames = new HashSet(utilisationMap.keySet());
        String newHostname = getHostname(999);
        dnsHostnames.add(newHostname); // simulated new SRV-Record
        CUT.updateDbServers(utilisedHostnames, dnsHostnames);
        verify(clusterMaintenanceTopic).publish(captor.capture());
        assertEquals(newHostname, captor.getValue().getHostname());
        assertEquals(ClusterEventType.HOST_APPEARED, captor.getValue().getType());
    }

    @Test
    public void hostDisappeared() {
        SrvRecordsTask CUT = new SrvRecordsTask(utilisationMap, clusterMaintenanceTopic);

        Set<String> utilisedHostnames = new HashSet(utilisationMap.keySet());
        String removedHostname = getHostname(3);
        dnsHostnames.remove(removedHostname); // simulate deleted SRV-Record
        CUT.updateDbServers(utilisedHostnames, dnsHostnames);
        verify(clusterMaintenanceTopic).publish(captor.capture());
        assertEquals(removedHostname, captor.getValue().getHostname());
        assertEquals(ClusterEventType.HOST_DISAPPEARED, captor.getValue().getType());
    }

    private String getHostname(int i) {
        return "host" + i + ".mydatabase.com";
    }

}
