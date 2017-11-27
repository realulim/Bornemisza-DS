package de.bornemisza.sessions.da;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.javalite.http.Post;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.rest.security.HashProvider;

public class SessionsServiceTest {
    
    private final BasicAuthCredentials creds = new BasicAuthCredentials("Tommy H.", "supersecretpw");

    private SessionsService CUT;
    private Post post;
    private final Map<String, List<String>> headers = new HashMap<>();
    private HashProvider hashProvider;

    @Before
    public void setUp() {
        String password = "My secret Password";
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn(password.toCharArray());
        hashProvider = new DbAdminPasswordBasedHashProvider(lbConfig);

        post = mock(Post.class);
        when(post.param(anyString(), any())).thenReturn(post);
        when(post.headers()).thenReturn(headers);
        Http http = mock(Http.class);
        when(http.post(anyString())).thenReturn(post);

        CouchSessionsPool pool = mock(CouchSessionsPool.class);
        HttpConnection conn = mock(HttpConnection.class);
        when(conn.getHttp()).thenReturn(http);
        when(pool.getConnection()).thenReturn(conn);

        lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn(password.toCharArray());
        CUT = new SessionsService(pool, lbConfig);
    }

    @Test
    public void createSession_technicalException() {
        String msg = "Connection refused";
        ConnectException cause = new ConnectException(msg);
        HttpException wrapperException = new HttpException(msg, cause);
        when(post.responseCode()).thenThrow(wrapperException);
        try {
            CUT.createSession(creds);
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void createSession_businessException() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(post.responseCode()).thenReturn(errorCode);
        when(post.responseMessage()).thenReturn(msg);
        try {
            CUT.createSession(creds);
            fail();
        }
        catch (BusinessException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void createSession_unauthorized() {
        int errorCode = 401;
        String msg = "Password wrong";
        when(post.responseCode()).thenReturn(errorCode);
        when(post.responseMessage()).thenReturn(msg);
        try {
            CUT.createSession(creds);
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void createNewSession_noCookie() {
        when(post.responseCode()).thenReturn(200);
        headers.put(HttpHeaders.SET_COOKIE, new ArrayList<>());
        assertNull(CUT.createSession(creds));
    }

    @Test
    public void createSession() {
        String cookie = "AuthSession=b866f6e2-be02-4ea0-99e6-34f989629930; Version=1; Path=/; HttpOnly; Secure";
        when(post.responseCode()).thenReturn(200);
        when(post.text()).thenReturn("{\"ok\":true,\"name\":\"Fazil Ongudar\",\"roles\":[\"customer\",\"user\"]}");
        List<String> cookies = new ArrayList<>();
        cookies.add(cookie);
        headers.put(HttpHeaders.SET_COOKIE, cookies);
        Session session = CUT.createSession(creds);
        assertNotNull(session);
        assertEquals(cookie, session.getDoubleSubmitToken().getCookie());
        assertEquals(hashProvider.hmacDigest(cookie.substring(0, cookie.indexOf(";"))), session.getDoubleSubmitToken().getCtoken());
    }

}
