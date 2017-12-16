package de.bornemisza.sessions.da;

import java.net.ConnectException;
import java.security.SecureRandom;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.entity.UuidsResult;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.sessions.boundary.SessionsType;

public class UuidsServiceTest {

    private final SecureRandom wheel = new SecureRandom();

    private UuidsService CUT;

    private CouchPool pool;
    private Http http;
    private HttpConnection conn;
    private Get get;
    private Auth auth;

    public UuidsServiceTest() {
    }
    
    @Before
    public void setUp() {
        pool = mock(CouchPool.class);
        conn = mock(HttpConnection.class);
        http = mock(Http.class);
        when(conn.getHttp()).thenReturn(http);
        when(pool.getConnection()).thenReturn(conn);
        
        get = mock(Get.class);
        when(get.header(anyString(), any())).thenReturn(get);
        when(http.get(anyString())).thenReturn(get);
        when(http.getBaseUrl()).thenReturn("http://db1.domain.de/foo"); // second DbServer
        auth = mock(Auth.class);

        CUT = new UuidsService(pool);
    }

    @Test
    public void getUuids_failed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(get.responseCode()).thenReturn(errorCode);
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.getUuids(auth, 3);
            fail();
        }
        catch (BusinessException ex) {
            assertEquals(SessionsType.UNEXPECTED, ex.getType());
            assertTrue(ex.getMessage().contains(errorCode + ":"));
        }
    }

    @Test
    public void getUuids_technicalError() {
        String msg = "Connection refused";
        ConnectException cause = new ConnectException(msg);
        HttpException wrapperException = new HttpException(msg, cause);
        when(get.responseCode()).thenThrow(wrapperException);
        try {
            CUT.getUuids(auth, 3);
            fail();
        }
        catch (TechnicalException ex) {
            assertEquals(wrapperException.toString(), ex.getMessage());
        }
    }

    @Test
    public void getUuids() {
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(getJson(0)).thenReturn(getJson(1)).thenReturn(getJson(2)).thenReturn(getJson(3));

        for (int i = 0; i < 4; i++) {
            UuidsResult result = CUT.getUuids(auth, i);
            assertEquals(i, result.getUuids().size());
        }
    }

    private String getJson(int count) {
        String json = "{\n" +
                      "    \"uuids\": [\n";
        for (int i = 0; i < count; i++) {
            int suffix = wheel.nextInt(100) + 100;
            String comma = count == (i + 1) ? "" : ",";
            json += "          \"6f4f195712bd76a67b2cba6737007" + suffix + "\"" + comma + "\n";
        }
        json +=       "    ]\n" +
                      "}";
        return json;
    }

}
