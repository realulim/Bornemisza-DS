package de.bornemisza.maintenance.task;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hazelcast.cache.ICache;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.ClusterEvent.ClusterEventType;
import de.bornemisza.maintenance.CouchUsersPool;
import de.bornemisza.maintenance.entity.PseudoHazelcastMap;
import de.bornemisza.maintenance.entity.PseudoHazelcastSet;

public class SrvRecordsTaskTest {

    private IMap utilisationMap;
    private List<String> dnsHostnames;
    private ITopic clusterMaintenanceTopic;
    private CouchUsersPool httpPool;
    private HealthChecks healthChecks;
    private ArgumentCaptor<ClusterEvent> captor;
    private HazelcastInstance hazelcast;
    private final ISet candidates = new PseudoHazelcastSet();
    private SrvRecordsTask CUT;

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

        httpPool = mock(CouchUsersPool.class);
        healthChecks = mock(HealthChecks.class);
        captor = ArgumentCaptor.forClass(ClusterEvent.class);

        hazelcast = mock(HazelcastInstance.class);
        clusterMaintenanceTopic = mock(ITopic.class);
        when(hazelcast.getMap(anyString())).thenReturn(utilisationMap);
        when(hazelcast.getReliableTopic(anyString())).thenReturn(clusterMaintenanceTopic);
        when(hazelcast.getSet(anyString())).thenReturn(candidates);

        when(hazelcast.getCacheManager()).thenReturn(new ICacheManager() {
            @Override
            public <K, V> ICache<K, V> getCache(String string) {
                return mock(ICache.class);
            }
        });

        CUT = new SrvRecordsTask(hazelcast, httpPool, healthChecks);
        CUT.init();
    }

    @Test
    public void candidateAppeared() {
        String newHostname = getHostname(999);
        Set<String> utilisedHostnames = new HashSet(utilisationMap.keySet());
        dnsHostnames.add(newHostname); // simulated new SRV-Record

        CUT.updateDbServers(utilisedHostnames, dnsHostnames);
        verifyNoMoreInteractions(clusterMaintenanceTopic);
        assertTrue(candidates.contains(newHostname));
    }

    @Test
    public void hostDisappeared() {
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
