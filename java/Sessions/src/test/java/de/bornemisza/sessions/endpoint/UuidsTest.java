package de.bornemisza.sessions.endpoint;


import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.sessions.boundary.UuidsFacade;

public class UuidsTest {
    
    private Uuids CUT;
    private UuidsFacade facade;

    public UuidsTest() {
    }
    
    @Before
    public void setUp() {
        facade = mock(UuidsFacade.class);
        CUT = new Uuids(facade);
    }

    @Test
    public void getUuids_unauthorized() {
        when(facade.getUuids(any(Auth.class), anyInt())).thenThrow(new UnauthorizedException("meh"));
        try {
            CUT.getUuids("someCookie", "someToken", 1);
            fail();
        }
        catch (RestException ex) {
            assertEquals(401, ex.getResponse().getStatus());
        }
    }
    
}
