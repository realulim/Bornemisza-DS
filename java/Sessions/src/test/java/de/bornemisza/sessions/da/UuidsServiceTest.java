package de.bornemisza.sessions.da;

import java.net.ConnectException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.result.UuidsResult;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.sessions.boundary.SessionsType;

public class UuidsServiceTest {

    private final SecureRandom wheel = new SecureRandom();

    private UuidsService CUT;

    private CouchPool pool;
    private Http http;
    private HttpConnection conn;
    private Get get;
    private final Map<String, List<String>> headers = new HashMap<>();

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
        when(get.headers()).thenReturn(headers);
        when(http.get(anyString())).thenReturn(get);
        when(http.getBaseUrl()).thenReturn("http://db1.domain.de/foo"); // second DbServer

        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn("My secret Password".toCharArray());

        CUT = new UuidsService(pool, lbConfig);
    }

    @Test
    public void getUuids_failed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(get.responseCode()).thenReturn(errorCode);
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.getUuids(3);
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
            CUT.getUuids(3);
            fail();
        }
        catch (TechnicalException ex) {
            assertEquals(wrapperException.toString(), ex.getMessage());
        }
    }

    @Test
    public void getUuids_noHeaders() {
        int count = 3;
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(getJson(count));
        when(get.headers()).thenReturn(new HashMap<>());

        UuidsResult result = CUT.getUuids(3);
        assertEquals(count, result.getUuids().size());
        assertNull(result.getFirstHeaderValue(HttpHeaders.BACKEND));
    }

    @Test
    public void getUuids() {
        String backendIp = "1.2.3.4";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(getJson(0)).thenReturn(getJson(1)).thenReturn(getJson(2)).thenReturn(getJson(3));
        Map<String, List<String>> backendHeaders = new HashMap<>();
        backendHeaders.put(HttpHeaders.BACKEND, Arrays.asList(new String[] { backendIp }));
        when(get.headers()).thenReturn(backendHeaders);

        for (int i = 0; i < 4; i++) {
            UuidsResult result = CUT.getUuids(i);
            assertEquals(i, result.getUuids().size());
            assertEquals(backendIp, result.getFirstHeaderValue(HttpHeaders.BACKEND));
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
