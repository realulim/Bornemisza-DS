package de.bornemisza.ds.sessions.da;

import de.bornemisza.ds.sessions.da.SessionsService;
import de.bornemisza.ds.sessions.da.CouchSessionsPool;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.javalite.http.Post;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
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
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.rest.security.HashProvider;

public class SessionsServiceTest {
    
    private final Auth auth = new Auth(new BasicAuthCredentials("Tommy Heelfigure", "supersecretpw"));

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
            CUT.createSession(auth);
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
            CUT.createSession(auth);
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
            CUT.createSession(auth);
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
        assertNull(CUT.createSession(auth));
    }

    @Test
    public void createSession() {
        String cookie = "AuthSession=RmF6aWwgT25ndWRhcjo1QTM2Nzc5Rg==; Version=1; Path=/; HttpOnly; Secure";
        when(post.responseCode()).thenReturn(200);
        when(post.text()).thenReturn("{\"ok\":true,\"name\":\"" + auth.getUsername() + "\",\"roles\":[\"customer\",\"user\"]}");
        List<String> cookies = new ArrayList<>();
        cookies.add(cookie);
        headers.put(HttpHeaders.SET_COOKIE, cookies);
        Session session = CUT.createSession(auth);
        assertNotNull(session);
        assertEquals(cookie, session.getDoubleSubmitToken().getCookie());
        assertTrue(cookie.contains(session.getDoubleSubmitToken().getBaseCookie()));
        assertEquals(hashProvider.encodeJasonWebToken(auth.getUsername()), session.getDoubleSubmitToken().getCtoken());
    }

}
