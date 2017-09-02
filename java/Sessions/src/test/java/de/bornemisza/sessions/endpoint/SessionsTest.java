package de.bornemisza.sessions.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.javalite.http.Get;
import org.javalite.http.Post;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.Http;
import de.bornemisza.rest.da.HttpPool;
import de.bornemisza.rest.entity.Session;

public class SessionsTest {

    private final String AUTH_HEADER = "Basic RmF6aWwgT25ndWRhcjpjaGFuZ2Vk";

    private Sessions CUT;
    private Http http;
    private Post post;
    private Get get;
    private final Map<String, List<String>> headers = new HashMap<>();

    @Before
    public void setUp() {
        post = mock(Post.class);
        when(post.param(anyString(), any())).thenReturn(post);
        when(post.headers()).thenReturn(headers);
        http = mock(Http.class);
        when(http.post(anyString())).thenReturn(post);

        get = mock(Get.class);
        when(get.header(anyString(), any())).thenReturn(get);
        when(http.get(anyString())).thenReturn(get);

        HttpPool pool = mock(HttpPool.class);
        when(pool.getConnection()).thenReturn(http);
        CUT = new Sessions(pool, pool);
    }

    @Test
    public void getNewSession_authHeaderMissing() {
        try {
            CUT.getNewSession(null);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(401, e.getResponse().getStatus());
        }
    }

    @Test
    public void getNewSession_postFailed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(post.responseCode()).thenReturn(errorCode);
        when(post.responseMessage()).thenReturn(msg);
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(errorCode, response.getStatus());
        assertEquals(msg, response.getEntity());
    }

    @Test
    public void getNewSession_noCookie() {
        when(post.responseCode()).thenReturn(200);
        headers.put(HttpHeaders.SET_COOKIE, new ArrayList<>());
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(500, response.getStatus());
        assertEquals("No Cookie!", response.getEntity());
    }

    @Test
    public void getNewSession() {
        String cookie = "My Cookie";
        when(post.responseCode()).thenReturn(200);
        List<String> cookies = new ArrayList<>();
        cookies.add(cookie);
        headers.put(HttpHeaders.SET_COOKIE, cookies);
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(200, response.getStatus());
        assertEquals(cookie, response.getHeaderString("Set-Cookie"));
    }

    @Test
    public void getActiveSession_noCookie() {
        try {
            CUT.getActiveSession("null");
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(401, e.getResponse().getStatus());
            assertEquals("No Cookie!", e.getResponse().getEntity());
        }
    }

    @Test
    public void getActiveSession_getFailed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(get.responseCode()).thenReturn(errorCode);
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.getActiveSession("MyCookie");
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(errorCode, e.getResponse().getStatus());
            assertEquals(msg, e.getResponse().getEntity());
        }
    }

    @Test
    public void getActiveSession_noJson() {
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn("not Json");
        try {
            CUT.getActiveSession("MyCookie");
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(500, e.getResponse().getStatus());
        }
    }

    @Test
    public void getActiveSession() {
        String json = "{\"ok\":true,\"userCtx\":{\"name\":\"Fazil Ongudar\",\"roles\":[\"customer\",\"user\"]},\"info\":{\"authentication_db\":\"_users\",\"authentication_handlers\":[\"cookie\",\"default\"],\"authenticated\":\"cookie\"}}";
        String cookie = "MyCookie";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        Session session = CUT.getActiveSession(cookie);
        assertEquals("Fazil Ongudar", session.getPrincipal());
        assertEquals(cookie, session.getCookie());
    }

    @Test
    public void deleteCookieInBrowser() {
        Response response = CUT.deleteCookieInBrowser("MyCookie");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void getUuids_noCookie() {
        try {
            CUT.getUuids("", 3);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(401, e.getResponse().getStatus());
            assertEquals("No Cookie!", e.getResponse().getEntity());
        }
    }

    @Test
    public void getUuids_getFailed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(get.responseCode()).thenReturn(errorCode);
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.getUuids("MyCookie", 3);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(errorCode, e.getResponse().getStatus());
            assertEquals(msg, e.getResponse().getEntity());
        }
    }

    @Test
    public void getUuids() {
        String json = "{\n" +
                      "    \"uuids\": [\n" +
                      "        \"6f4f195712bd76a67b2cba6737007f44\",\n" +
                      "        \"6f4f195712bd76a67b2cba6737008c8a\",\n" +
                      "        \"6f4f195712bd76a67b2cba6737009adb\"\n" +
                      "    ]\n" +
                      "}";
        String cookie = "MyCookie";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        Response response = CUT.getUuids(cookie, 3);
        assertEquals(200, response.getStatus());
        assertEquals(json, response.getEntity());
    }

}
