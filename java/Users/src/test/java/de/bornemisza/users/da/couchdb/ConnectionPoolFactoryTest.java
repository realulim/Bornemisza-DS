package de.bornemisza.users.da.couchdb;

import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import de.bornemisza.users.entity.SrvRecord;

public class ConnectionPoolFactoryTest {

    private final String service = "_db._tcp.somedomain.com";

    @Test
    public void getSrvRecordsSortedByPriority_noServiceFound() throws Exception {
        try {
            ConnectionPoolFactory CUT = new ConnectionPoolFactory();
            DirContext ctx = mock(DirContext.class);
            NamingEnumeration<?> enumeration = createEnumerationMock(ctx);

            when(enumeration.hasMore()).thenReturn(false);
            CUT.retrieveSrvRecordsAndSort(ctx, service);
            fail();
        }
        catch (NamingException ne) {
            assertEquals("No Service " + service + " found!", ne.getMessage());
        }
    }

    @Test
    public void getSrvRecordsSortedByPriority() throws Exception {
        ConnectionPoolFactory CUT = new ConnectionPoolFactory();
        DirContext ctx = mock(DirContext.class);
        NamingEnumeration<?> enumeration = createEnumerationMock(ctx);
        when(enumeration.hasMore()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(enumeration.next()).thenReturn("25 0 443 db23.somedomain.com.").thenReturn("5 0 443 db7.somedomain.com.").thenReturn("15 0 443 db3.somedomain.com.");
        List<SrvRecord> srvRecords = CUT.retrieveSrvRecordsAndSort(ctx, service);
        int lastPriority = -1;
        for (SrvRecord record : srvRecords) {
            assertTrue(lastPriority < record.getPriority());
            lastPriority = record.getPriority();
        }
    }

    private NamingEnumeration<?> createEnumerationMock(DirContext ctx) throws NamingException {
        Attributes atts = mock(Attributes.class);
        Attribute att = mock(Attribute.class);
        NamingEnumeration enumeration = mock(NamingEnumeration.class);
        when(att.getAll()).thenReturn(enumeration);
        when(atts.get("srv")).thenReturn(att);
        when(ctx.getAttributes(anyString(), any(String[].class))).thenReturn(atts);
        return enumeration;
    }
    
}
