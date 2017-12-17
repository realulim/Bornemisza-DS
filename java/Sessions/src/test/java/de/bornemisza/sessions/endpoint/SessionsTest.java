package de.bornemisza.sessions.endpoint;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.sessions.boundary.SessionsFacade;

public class SessionsTest {

    private final String AUTH_HEADER = "Basic RmF6aWwgT25ndWRhcjpjaGFuZ2Vk";

    private Sessions CUT;
    private SessionsFacade facade;

    @Before
    public void setUp() {
        facade = mock(SessionsFacade.class);
        CUT = new Sessions(facade);
    }

    @Test
    public void getNewSession_authHeaderMissing() {
        when(facade.createNewSession(null)).thenThrow(new UnauthorizedException("No way"));
        Response response = CUT.getNewSession(null);
        assertEquals(401, response.getStatus());
    }

    @Test
    public void getNewSession_runtimeException() {
        String msg = "Connection refused";
        when(facade.createNewSession(AUTH_HEADER)).thenThrow(new TechnicalException(msg));
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(500, response.getStatus());
        assertTrue(response.getEntity().toString().contains(msg));
    }

    @Test
    public void getNewSession_noCookie() {
        when(facade.createNewSession(AUTH_HEADER)).thenReturn(null);
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(404, response.getStatus());
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
        assertEquals(ctoken, response.getHeaderString(HttpHeaders.CTOKEN));
    }

    @Test
    public void deleteCookieInBrowser() {
        Response response = CUT.deleteCookieInBrowser();
        assertEquals(200, response.getStatus());
    }

}
