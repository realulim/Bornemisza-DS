package de.bornemisza.ds.maintenance.task;

import de.bornemisza.ds.maintenance.task.HealthChecks;
import de.bornemisza.ds.maintenance.task.HealthCheckTask;
import java.util.List;

import com.hazelcast.core.HazelcastInstance;
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
import de.bornemisza.loadbalancer.Config;
import de.bornemisza.ds.maintenance.CouchUsersPool;
import de.bornemisza.ds.maintenance.entity.PseudoHazelcastMap;
import de.bornemisza.ds.maintenance.entity.PseudoHazelcastSet;
import de.bornemisza.rest.HttpConnection;

public class HealthCheckTaskTest {

    private String hostname;
    private final IMap connections = new PseudoHazelcastMap();
    private ITopic clusterMaintenanceTopic;
    private HealthChecks healthChecks;
    private HazelcastInstance hazelcast;
    private CouchUsersPool httpPool;
    private ArgumentCaptor<ClusterEvent> captor;
    private HealthCheckTask CUT;
    
    public HealthCheckTaskTest() {
    }
    
    @Before
    public void setUp() {
        hazelcast = mock(HazelcastInstance.class);
        clusterMaintenanceTopic = mock(ITopic.class);
        when(hazelcast.getReliableTopic(anyString())).thenReturn(clusterMaintenanceTopic);

        hostname = "db-1.domain.de";
        connections.put(hostname, mock(HttpConnection.class));

        httpPool = mock(CouchUsersPool.class);
        when(httpPool.getAllConnections()).thenReturn(connections);
        healthChecks = mock(HealthChecks.class);
        captor = ArgumentCaptor.forClass(ClusterEvent.class);

        CUT = new HealthCheckTask(hazelcast, httpPool, clusterMaintenanceTopic, healthChecks);
    }

    @Test
    public void healthChecks() {
        ISet candidates = new PseudoHazelcastSet();
        when(hazelcast.getSet(Config.CANDIDATES)).thenReturn(candidates);
        when(healthChecks.isCouchDbReady(any(HttpConnection.class)))
                .thenReturn(true)   // start out healthy, nothing happens
                .thenReturn(false)  // first failure, first alert
                .thenReturn(false)  // second failure, no alert
                .thenReturn(false)  // third failure, second alert
                .thenReturn(true)   // first unalert
                .thenReturn(false)  // fifth failure, third alert
                .thenReturn(true)   // second unalert
                .thenReturn(true);  // still healthy, nothing happens

        CUT.healthChecks();
        verifyNoMoreInteractions(clusterMaintenanceTopic);

        CUT.healthChecks();
        verify(clusterMaintenanceTopic, times(1)).publish(captor.capture());
        checkPublishedMessages(ClusterEventType.HOST_UNHEALTHY);

        CUT.healthChecks();
        verifyNoMoreInteractions(clusterMaintenanceTopic);

        CUT.healthChecks();
        verify(clusterMaintenanceTopic, times(2)).publish(captor.capture());
        checkPublishedMessages(ClusterEventType.HOST_UNHEALTHY);

        CUT.healthChecks();
        verify(clusterMaintenanceTopic, times(3)).publish(captor.capture());
        checkPublishedMessages(ClusterEventType.HOST_HEALTHY);

        CUT.healthChecks();
        verify(clusterMaintenanceTopic, times(4)).publish(captor.capture());
        checkPublishedMessages(ClusterEventType.HOST_UNHEALTHY);

        CUT.healthChecks();
        verify(clusterMaintenanceTopic, times(5)).publish(captor.capture());
        checkPublishedMessages(ClusterEventType.HOST_HEALTHY);

        CUT.healthChecks();
        verifyNoMoreInteractions(clusterMaintenanceTopic);
    }

    private void checkPublishedMessages(ClusterEventType type) {
        assertEquals(hostname, captor.getAllValues().get(captor.getAllValues().size() - 1).getHostname());
        assertEquals(type, captor.getAllValues().get(captor.getAllValues().size() - 1).getType());
    }

    @Test
    public void healthChecks_candidateUnhealthy() {
        connections.clear();
        ISet candidates = new PseudoHazelcastSet();
        candidates.add(hostname);
        when(hazelcast.getSet(Config.CANDIDATES)).thenReturn(candidates);
        when(healthChecks.isCouchDbReady(any(HttpConnection.class))).thenReturn(false);

        CUT.healthChecks();
        verifyNoMoreInteractions(clusterMaintenanceTopic);
    }

    @Test
    public void healthChecks_candidateHealthy() {
        connections.clear();
        ISet candidates = new PseudoHazelcastSet();
        candidates.add(hostname);
        when(hazelcast.getSet(Config.CANDIDATES)).thenReturn(candidates);
        when(healthChecks.isCouchDbReady(any())).thenReturn(true);

        CUT.healthChecks();
        verify(clusterMaintenanceTopic, times(1)).publish(captor.capture());
        List<ClusterEvent> clusterEvents = captor.getAllValues();
        assertEquals(1, clusterEvents.size());
        assertEquals(hostname, clusterEvents.get(0).getHostname());
        assertEquals(ClusterEventType.CANDIDATE_HEALTHY, clusterEvents.get(0).getType());
    }

    @Test
    public void healthChecks_technicalErrorWhileCheckingCandidates() {
        connections.clear();
        ISet candidates = new PseudoHazelcastSet();
        candidates.add(hostname);
        when(hazelcast.getSet(Config.CANDIDATES)).thenReturn(candidates);
        when(httpPool.createConnection(anyString())).thenThrow(new RuntimeException("Connection refused"));

        CUT.healthChecks();
        verifyNoMoreInteractions(clusterMaintenanceTopic);
    }

}
