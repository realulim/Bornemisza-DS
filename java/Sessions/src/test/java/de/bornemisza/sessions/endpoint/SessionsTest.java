package de.bornemisza.sessions.endpoint;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.javalite.http.Get;
import org.javalite.http.HttpException;
import org.javalite.http.Post;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.Http;
import de.bornemisza.sessions.da.HttpSessionsPool;
import de.bornemisza.sessions.security.HashProvider;

public class SessionsTest {

    private final String AUTH_HEADER = "Basic RmF6aWwgT25ndWRhcjpjaGFuZ2Vk";

    private Sessions CUT;
    private Http http;
    private Post post;
    private Get get;
    private final Map<String, List<String>> headers = new HashMap<>();
    private HttpSessionsPool pool;
    private HashProvider hashProvider;

    @Before
    public void setUp() {
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn("My Secret Password".toCharArray());
        hashProvider = new HashProvider(lbConfig);

        post = mock(Post.class);
        when(post.param(anyString(), any())).thenReturn(post);
        when(post.headers()).thenReturn(headers);
        http = mock(Http.class);
        when(http.post(anyString())).thenReturn(post);

        get = mock(Get.class);
        when(get.header(anyString(), any())).thenReturn(get);
        when(http.get(anyString())).thenReturn(get);

        pool = mock(HttpSessionsPool.class);
        when(pool.getConnection()).thenReturn(http);

        CUT = new Sessions(pool, hashProvider);
    }

    @Test
    public void getNewSession_technicalError() {
        String msg = "Connection refused";
        ConnectException cause = new ConnectException(msg);
        HttpException wrapperException = new HttpException(msg, cause);
        when(post.responseCode()).thenThrow(wrapperException);
        Response resp = CUT.getNewSession(AUTH_HEADER);
        assertEquals(500, resp.getStatus());
        assertEquals(wrapperException.toString(), resp.getEntity());
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
        String cookie = "AuthSession=b866f6e2-be02-4ea0-99e6-34f989629930; Version=1; Path=/; HttpOnly; Secure";
        when(post.responseCode()).thenReturn(200);
        List<String> cookies = new ArrayList<>();
        cookies.add(cookie);
        headers.put(HttpHeaders.SET_COOKIE, cookies);
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(200, response.getStatus());
        assertEquals(cookie, response.getHeaderString(HttpHeaders.SET_COOKIE));
        assertEquals(hashProvider.hmacDigest(cookie.substring(0, cookie.indexOf(";"))), response.getHeaderString(Sessions.CTOKEN));
    }

    @Test
    public void deleteCookieInBrowser() {
        Response response = CUT.deleteCookieInBrowser();
        assertEquals(200, response.getStatus());
    }

}
