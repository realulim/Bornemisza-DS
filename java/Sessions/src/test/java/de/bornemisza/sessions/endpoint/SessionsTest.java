package de.bornemisza.sessions.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.sessions.boundary.SessionsFacade;
import de.bornemisza.sessions.boundary.TechnicalException;
import de.bornemisza.sessions.boundary.UnauthorizedException;

public class SessionsTest {

    private final String AUTH_HEADER = "Basic RmF6aWwgT25ndWRhcjpjaGFuZ2Vk";

    private Sessions CUT;
    private SessionsFacade facade;
    private final Map<String, List<String>> headers = new HashMap<>();

    @Before
    public void setUp() {
        facade = mock(SessionsFacade.class);
        CUT = new Sessions(facade);
    }

    @Test
    public void getNewSession_authHeaderMissing() {
        try {
            when(facade.createNewSession(null)).thenThrow(new UnauthorizedException("No way José"));
            CUT.getNewSession(null);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(401, e.getResponse().getStatus());
        }
    }

    @Test
    public void getNewSession_runtimeException() {
        String msg = "Connection refused";
        when(facade.createNewSession(AUTH_HEADER)).thenThrow(new TechnicalException(msg));
        try {
            CUT.getNewSession(AUTH_HEADER);
            fail();
        }
        catch (RestException ex) {
            assertTrue(ex.getResponse().getEntity().toString().contains(msg));
        }
    }

    @Test
    public void getNewSession_noCookie() {
        when(facade.createNewSession(AUTH_HEADER)).thenReturn(null);
        try {
            CUT.getNewSession(AUTH_HEADER);
            fail();
        }
        catch (RestException ex) {
            assertEquals(404, ex.getResponse().getStatus());
        }
    }

    @Test
    public void getNewSession() {
        String cookie = "AuthSession=b866f6e2-be02-4ea0-99e6-34f989629930; Version=1; Path=/; HttpOnly; Secure";
        String ctoken = "some-random-string";
        DoubleSubmitToken dsToken = new DoubleSubmitToken(cookie, ctoken);
        Session session = new Session();
        session.setDoubleSubmitToken(dsToken);
        when(facade.createNewSession(AUTH_HEADER)).thenReturn(session);
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(200, response.getStatus());
        assertEquals(cookie, response.getHeaderString(HttpHeaders.SET_COOKIE));
        assertEquals(ctoken, response.getHeaderString(Sessions.CTOKEN_HEADER));
    }

    @Test
    public void deleteCookieInBrowser() {
        Response response = CUT.deleteCookieInBrowser();
        assertEquals(200, response.getStatus());
    }

}
