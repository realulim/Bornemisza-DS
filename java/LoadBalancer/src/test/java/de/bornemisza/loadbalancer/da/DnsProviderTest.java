package de.bornemisza.loadbalancer.da;

import java.util.Arrays;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import com.hazelcast.cache.ICache;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.entity.SrvRecord;

public class DnsProviderTest {
    
    private final String service = "_db._tcp.somedomain.com";
    private DnsProvider CUT;

    private ICache cache;
    private NamingEnumeration<?> enumeration;

    @Before
    public void setUp() throws NamingException {
        HazelcastInstance hazelcast = mock(HazelcastInstance.class);
        ICacheManager cacheManager = mock(ICacheManager.class);
        cache = mock(ICache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(hazelcast.getCacheManager()).thenReturn(cacheManager);

        DirContext dirContext = mock(DirContext.class);
        enumeration = createEnumerationMock(dirContext);

        System.setProperty("DNSRESOLVER", "ns.someprovider.com");
        System.setProperty("FQDN", "www.somedomain.com");
        this.CUT = new DnsProvider(hazelcast, dirContext);
    }

    @Test
    public void getHostnamesForService_cacheHit() {
        when(cache.get(any())).thenReturn(getHostnames());
        List<String> hostnamesForService = CUT.getHostnamesForService(service);
        assertEquals(getHostnames(), hostnamesForService);
    }

    @Test
    public void getHostnamesForService_cacheEmpty() throws NamingException {
        when(cache.get(any())).thenReturn(null);
        when(enumeration.hasMore()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(enumeration.next()).thenReturn("25 0 443 db23.somedomain.com.").thenReturn("5 0 443 db7.somedomain.com.").thenReturn("15 0 443 db3.somedomain.com.");
        List<String> hostnamesForService = CUT.getHostnamesForService(service);
        assertEquals(getHostnames(), hostnamesForService);
    }

    @Test
    public void getHostnamesForService_cacheEmpty_thenException() throws NamingException {
        when(cache.get(any())).thenReturn(null);
        when(enumeration.hasMore()).thenReturn(true).thenReturn(false).thenReturn(true);
        when(enumeration.next()).thenReturn("25 0 443 db23.somedomain.com.").thenThrow(new NamingException("Connection dead"));
        List<String> hostnamesForService = CUT.getHostnamesForService(service);
        List<String> cachedHostnamesForService = CUT.getHostnamesForService(service);
        assertEquals(hostnamesForService, cachedHostnamesForService);
    }

    @Test
    public void getSrvRecordsSortedByPriority_noServiceFound() throws Exception {
        when(enumeration.hasMore()).thenReturn(false);
        try {
            CUT.retrieveSrvRecordsAndSort(service);
            fail();
        }
        catch (NamingException ne) {
            assertEquals("No Service " + service + " found!", ne.getMessage());
        }
    }

    @Test
    public void getSrvRecordsSortedByPriority() throws Exception {
        when(enumeration.hasMore()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(enumeration.next()).thenReturn("25 0 443 db23.somedomain.com.").thenReturn("5 0 443 db7.somedomain.com.").thenReturn("15 0 443 db3.somedomain.com.");
        List<SrvRecord> srvRecords = CUT.retrieveSrvRecordsAndSort(service);
        int lastPriority = -1;
        for (SrvRecord record : srvRecords) {
            assertTrue(lastPriority < record.getPriority());
            lastPriority = record.getPriority();
        }
    }

    private NamingEnumeration<?> createEnumerationMock(DirContext ctx) throws NamingException {
        Attributes atts = mock(Attributes.class);
        Attribute att = mock(Attribute.class);
        NamingEnumeration enumer = mock(NamingEnumeration.class);
        when(att.getAll()).thenReturn(enumer);
        when(atts.get("srv")).thenReturn(att);
        when(ctx.getAttributes(anyString(), any(String[].class))).thenReturn(atts);
        return enumer;
    }

    private List<String> getHostnames() {
        return Arrays.asList(new String[] { "db7.somedomain.com", "db3.somedomain.com", "db23.somedomain.com" });
    }
}
