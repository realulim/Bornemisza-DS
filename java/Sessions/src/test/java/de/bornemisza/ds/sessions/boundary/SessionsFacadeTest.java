package de.bornemisza.ds.sessions.boundary;


import de.bornemisza.ds.sessions.boundary.SessionsFacade;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.ds.sessions.da.SessionsService;

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
        when(sessionsService.createSession(any(Auth.class))).thenReturn(new Session());
        assertNotNull(CUT.createNewSession(AUTH_HEADER));
    }

}
