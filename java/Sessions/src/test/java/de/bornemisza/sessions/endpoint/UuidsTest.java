package de.bornemisza.sessions.endpoint;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.sessions.boundary.UuidsFacade;
import javax.ws.rs.core.Response;

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
        Response response = CUT.getUuids("someCookie", "someToken", 1);
        assertEquals(401, response.getStatus());
    }
    
}
