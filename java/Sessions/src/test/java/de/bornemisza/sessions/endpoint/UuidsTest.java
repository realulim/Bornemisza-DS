package de.bornemisza.sessions.endpoint;


import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.BusinessExceptionType;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.sessions.boundary.SessionsType;
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
        Response response = CUT.getUuids("someCookie", "someToken", 1);
        assertEquals(401, response.getStatus());
    }

    @Test
    public void getUuids_technicalException() {
        when(facade.getUuids(any(Auth.class), anyInt())).thenThrow(new TechnicalException("Connection refused"));
        Response response = CUT.getUuids("someCookie", "someToken", 1);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void getUuids_businessException() {
        when(facade.getUuids(any(Auth.class), anyInt())).thenThrow(new BusinessException(SessionsType.UNEXPECTED, "Bandwidth Limit Exceeded"));
        Response response = CUT.getUuids("someCookie", "someToken", 1);
        assertEquals(500, response.getStatus());
    }

}
