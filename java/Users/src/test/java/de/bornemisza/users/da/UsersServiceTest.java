package de.bornemisza.users.da;



import javax.ws.rs.core.MediaType;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.users.boundary.BusinessException;
import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.boundary.TechnicalException;

public class UsersServiceTest {

    private UsersService CUT;
    private Get get;
    private int responseCode;

    public UsersServiceTest() {
    }

    @Before
    public void setUp() {
        CouchUsersPoolAsAdmin adminPool = mock(CouchUsersPoolAsAdmin.class);
        CouchUsersPool usersPool = mock(CouchUsersPool.class);

        Http http = mock(Http.class);
        when(http.getBaseUrl()).thenReturn("https://host.domain.com/myurl");
        when(http.getHostName()).thenReturn("host.domain.com");
        get = mock(Get.class);
        when(get.header(anyString(), anyString())).thenReturn(get);
        when(http.get(anyString())).thenReturn(get);
        when(usersPool.getConnection()).thenReturn(new HttpConnection("myDb", http));
        when(adminPool.getConnection()).thenReturn(new HttpConnection("myDb", http));
        CUT = new UsersService(adminPool, usersPool);
    }

    @Test
    public void init_technicalException() {
        String errMsg = "Connection refused";
        when(get.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.init();
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void init_businessException() {
        when(get.responseCode()).thenReturn(500);
        try {
            CUT.init();
            fail();
        }
        catch (BusinessException e) {
            assertEquals(Type.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void init() {
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn("{\"db_name\":\"_users\",\"update_seq\":\"27-g1AAAAFTeJzLYWBg4MhgTmEQTM4vTc5ISXIwNNAz0TPQszDLAUoxJTIkyf___z8rkQGPoiQFIJlkT1idA0hdPFgdIz51CSB19QTNy2MBkgwNQAqodH5WoiRBtQsgavcTY-4BiNr7-N0KUfsAohbk3iwAlrhe6Q\",\"sizes\":{\"file\":144661,\"external\":5675,\"active\":10411},\"purge_seq\":0,\"other\":{\"data_size\":5675},\"doc_del_count\":1,\"doc_count\":3,\"disk_size\":144661,\"disk_format_version\":6,\"data_size\":10411,\"compact_running\":false,\"instance_start_time\":\"0\"}");
        CUT.init();
        verify(get).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    }

}
