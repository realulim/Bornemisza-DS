package de.bornemisza.sessions.boundary;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.sessions.da.SessionsService;

public class SessionsFacadeTest {
    
    private SessionsFacade CUT;
    private SessionsService sessionsService;

    private final String AUTH_HEADER = "Basic RmF6aWwgT25ndWRhcjpjaGFuZ2Vk";

    @Before
    public void setUp() {
        sessionsService = mock(SessionsService.class);
        CUT = new SessionsFacade(sessionsService);
    }

    @Test
    public void createNewSession_unauthorized() {
        try {
            CUT.createNewSession("Basic boofar");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().contains("unparseable"));
        }
    }

    @Test
    public void createNewSession() {
        when(sessionsService.createSession(any(BasicAuthCredentials.class))).thenReturn(new Session());
        assertNotNull(CUT.createNewSession(AUTH_HEADER));
    }

}
