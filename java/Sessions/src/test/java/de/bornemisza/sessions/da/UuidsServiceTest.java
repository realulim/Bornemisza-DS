package de.bornemisza.sessions.da;

import java.net.ConnectException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.javalite.http.Post;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.Uuid;
import de.bornemisza.rest.entity.result.KeyValueViewResult;
import de.bornemisza.rest.entity.result.RestResult;
import de.bornemisza.rest.entity.result.UuidsResult;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.sessions.boundary.SessionsType;

public class UuidsServiceTest {

    private final SecureRandom wheel = new SecureRandom();

    private UuidsService CUT;

    private CouchPool pool;
    private Http http;
    private HttpConnection conn;
    private Get get;
    private Post post;
    private final Map<String, List<String>> headers = new HashMap<>();
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
        when(get.headers()).thenReturn(headers);
        when(http.get(anyString())).thenReturn(get);

        post = mock(Post.class);
        when(post.header(anyString(), anyString())).thenReturn(post);
        when(post.headers()).thenReturn(headers);
        when(http.post(anyString(), anyString())).thenReturn(post);

        auth = mock(Auth.class);
        when(auth.getCookie()).thenReturn("SomeCookie");

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
            assertEquals(SessionsType.GETUUIDS, ex.getType());
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
        when(get.text()).thenReturn(getUuidResultAsJson(count));
        when(get.headers()).thenReturn(new HashMap<>());

        UuidsResult result = CUT.getUuids(3);
        assertEquals(count, result.getUuids().size());
        assertNull(result.getFirstHeaderValue(HttpHeaders.BACKEND));
    }

    @Test
    public void getUuids() {
        String backendIp = "1.2.3.4";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(getUuidResultAsJson(0)).thenReturn(getUuidResultAsJson(1)).thenReturn(getUuidResultAsJson(2)).thenReturn(getUuidResultAsJson(3));
        Map<String, List<String>> backendHeaders = new HashMap<>();
        backendHeaders.put(HttpHeaders.BACKEND, Arrays.asList(new String[] { backendIp }));
        when(get.headers()).thenReturn(backendHeaders);

        for (int i = 0; i < 4; i++) {
            UuidsResult result = CUT.getUuids(i);
            assertEquals(i, result.getUuids().size());
            assertEquals(backendIp, result.getFirstHeaderValue(HttpHeaders.BACKEND));
        }
    }

    @Test
    public void saveUuids_unauthorized() {
        int errorCode = 401;
        String msg = "Unauthorized";
        when(post.responseCode()).thenReturn(errorCode);
        when(post.responseMessage()).thenReturn(msg);
        try {
            CUT.saveUuids(auth, "userDatabase", createUuid());
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void saveUuids_technicalError() {
        String msg = "Connection refused";
        ConnectException cause = new ConnectException(msg);
        HttpException wrapperException = new HttpException(msg, cause);
        when(post.responseCode()).thenThrow(wrapperException);
        try {
            CUT.saveUuids(auth, "userDatabase", createUuid());
            fail();
        }
        catch (TechnicalException ex) {
            assertEquals(wrapperException.toString(), ex.getMessage());
        }
    }

    @Test
    public void saveUuids_businessError() {
        int statusCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(post.responseCode()).thenReturn(statusCode);
        when(post.responseMessage()).thenReturn(msg);
        try {
            CUT.saveUuids(auth, "userDatabase", createUuid());
            fail();
        }
        catch (BusinessException ex) {
            assertEquals(SessionsType.SAVEUUIDS, ex.getType());
            assertTrue(ex.getMessage().contains(statusCode + ":"));
        }
    }

    @Test
    public void saveUuids() {
        int statusCode = 201;
        String cookie = "NewCookie";
        headers.put(HttpHeaders.SET_COOKIE, Arrays.asList(new String[] { cookie }));
        when(post.responseCode()).thenReturn(statusCode);
        RestResult result = CUT.saveUuids(auth, "userDatabase", createUuid());
        assertEquals(headers, result.getHeaders());
        assertEquals(cookie, result.getNewCookie());
    }

    @Test
    public void loadColors_unauthorized() {
        int errorCode = 401;
        String msg = "Unauthorized";
        when(get.responseCode()).thenReturn(errorCode);
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.loadColors(auth, "userDatabase");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void loadColors_technicalError() {
        String msg = "Connection refused";
        ConnectException cause = new ConnectException(msg);
        HttpException wrapperException = new HttpException(msg, cause);
        when(get.responseCode()).thenThrow(wrapperException);
        try {
            CUT.loadColors(auth, "userDatabase");
            fail();
        }
        catch (TechnicalException ex) {
            assertEquals(wrapperException.toString(), ex.getMessage());
        }
    }

    @Test
    public void loadColors_businessError() {
        int statusCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(get.responseCode()).thenReturn(statusCode);
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.loadColors(auth, "userDatabase");
            fail();
        }
        catch (BusinessException ex) {
            assertEquals(SessionsType.LOADCOLORS, ex.getType());
            assertTrue(ex.getMessage().contains(statusCode + ":"));
        }
    }

    @Test
    public void loadColors() {
        String cookie = "Cookie";
        int crimsonCount = 2846;
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(getColorsResultAsJson(crimsonCount));
        headers.put(HttpHeaders.SET_COOKIE, Arrays.asList(new String[] { cookie }));
        KeyValueViewResult result = CUT.loadColors(auth, "userDatabase");
        assertEquals(headers, result.getHeaders());
        assertEquals(cookie, result.getNewCookie());
        assertEquals(5, result.getRows().size());
        assertEquals(crimsonCount + "", result.getRows().get(1).getValue());
        assertEquals((crimsonCount + 4000 - 145) + "", result.getRows().get(4).getValue());
    }

    private String getColorsResultAsJson(int crimsonCount) {
        return  " {\"rows\":[\n" +
                "        {\"key\":\"app-Black\",\"value\":145},\n" +
                "        {\"key\":\"app-Crimson\",\"value\":" + crimsonCount + "},\n" +
                "        {\"key\":\"app-LightSeaGreen\",\"value\":8087},\n" +
                "        {\"key\":\"db-LightSeaGreen\",\"value\":4087},\n" +
                "        {\"key\":\"db-RoyalBlue\",\"value\":" + (crimsonCount + 4000 - 145) + "}\n" +
                " ]}\n";
    }

    private String getUuidResultAsJson(int count) {
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

    private Uuid createUuid() {
        Uuid uuid = new Uuid();
        uuid.setValues(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737007", "f4c278d2b6f17060430c8f28d2ec26cc", "430c8f4c278d2b6f17060f28d2ec83d4" }));
        uuid.setDbColor("yellow");
        uuid.setDate(LocalDate.now());
        return uuid;
    }

}
