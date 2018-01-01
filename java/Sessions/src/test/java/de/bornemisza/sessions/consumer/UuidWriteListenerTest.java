package de.bornemisza.sessions.consumer;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemEvent;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.entity.Uuid;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.sessions.da.UuidsService;
import java.util.concurrent.TimeUnit;

public class UuidWriteListenerTest {

    private UuidWriteListener CUT;
    private UuidsService uuidsService;
    private HazelcastInstance hazelcast;
    private String cookie, jwt;
    private Auth auth;
    private IQueue uuidWriteQueue;
    
    public UuidWriteListenerTest() {
    }
    
    @Before
    public void setUp() {
        cookie = "AuthSession=RmF6aWwgT25ndWRhcjo1QTM2Nzc5Rg==";
        jwt = "someHash";
        DoubleSubmitToken dsToken = new DoubleSubmitToken(cookie, jwt);
        auth = new Auth(dsToken);

        hazelcast = mock(HazelcastInstance.class);
        this.uuidWriteQueue = mock(IQueue.class);
        when(hazelcast.getQueue(anyString())).thenReturn(uuidWriteQueue);

        uuidsService = mock(UuidsService.class);
        CUT = new UuidWriteListener(hazelcast, uuidsService);
    }

    @Test
    public void itemAdded_noneAvailable() throws Exception {
        when(uuidWriteQueue.poll(anyLong(), any(TimeUnit.class))).thenReturn(null);
        ItemEvent itemEvent = mock(ItemEvent.class);
        CUT.itemAdded(itemEvent);
        verifyNoMoreInteractions(uuidsService);
        verify(uuidWriteQueue).addItemListener(CUT, false);
        verify(uuidWriteQueue).poll(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(uuidWriteQueue);
    }

    @Test
    public void itemAdded_interrupted() throws Exception{
        when(uuidWriteQueue.poll(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());
        ItemEvent itemEvent = mock(ItemEvent.class);
        CUT.itemAdded(itemEvent);
        verifyNoMoreInteractions(uuidsService);
        verify(uuidWriteQueue).addItemListener(CUT, false);
        verify(uuidWriteQueue).poll(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(uuidWriteQueue);
    }

    @Test
    public void itemAdded() throws Exception {
        String dbName = "someDatabase";
        Uuid uuidDocument = mock(Uuid.class);
        StoreUuidRequest request = new StoreUuidRequest(auth, dbName, uuidDocument);
        when(uuidWriteQueue.poll(anyLong(), any(TimeUnit.class))).thenReturn(request);
        ItemEvent itemEvent = mock(ItemEvent.class);
        CUT.itemAdded(itemEvent);
        verify(uuidsService).saveUuids(auth, dbName, uuidDocument);
        verifyNoMoreInteractions(uuidsService);
        verify(uuidWriteQueue).addItemListener(CUT, false);
        verify(uuidWriteQueue).poll(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(uuidWriteQueue);
    }

    @Test
    public void itemRemoved() throws Exception {
        ItemEvent itemEvent = mock(ItemEvent.class);
        CUT.itemRemoved(itemEvent);
        verifyNoMoreInteractions(uuidsService);
        verify(uuidWriteQueue).addItemListener(CUT, false);
        verifyNoMoreInteractions(uuidWriteQueue);
    }
    
}
