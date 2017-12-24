package de.bornemisza.sessions.endpoint;




import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.entity.result.KeyValueViewResult;
import de.bornemisza.rest.entity.result.UuidsResult;
import de.bornemisza.rest.exception.BusinessException;
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
    public void getUuids_illegalCount() {
        Response response = CUT.getUuids("someCookie", "someToken", 0);
        assertEquals(400, response.getStatus());
        response = CUT.getUuids("someCookie", "someToken", -1);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void getUuids_businessException() {
        when(facade.getUuids(any(Auth.class), anyInt())).thenThrow(new BusinessException(SessionsType.UNEXPECTED, "Bandwidth Limit Exceeded"));
        Response response = CUT.getUuids("someCookie", "someToken", 1);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void getUuids() {
        UuidsResult result = new UuidsResult();
        result.setStatus(Status.OK);
        when(facade.getUuids(any(Auth.class), anyInt())).thenReturn(result);
        Response response = CUT.getUuids("someCookie", "someToken", 1);
        assertEquals(result, response.getEntity());
    }

    @Test
    public void loadColors_unauthorized() {
        when(facade.loadColors(any(Auth.class))).thenThrow(new UnauthorizedException("nah"));
        Response response = CUT.loadColors("someCookie", "someToken");
        assertEquals(401, response.getStatus());
    }

    @Test
    public void loadColors_technicalException() {
        when(facade.loadColors(any(Auth.class))).thenThrow(new TechnicalException("Connection refused"));
        Response response = CUT.loadColors("someCookie", "someToken");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void loadColors_businessException() {
        when(facade.loadColors(any(Auth.class))).thenThrow(new BusinessException(SessionsType.UNEXPECTED, "Bandwidth Limit Exceeded"));
        Response response = CUT.loadColors("someCookie", "someToken");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void loadColors() {
        KeyValueViewResult result = new KeyValueViewResult();
        result.setStatus(Status.OK);
        when(facade.loadColors(any(Auth.class))).thenReturn(result);
        Response response = CUT.loadColors("someCookie", "someToken");
        assertEquals(result, response.getEntity());
    }

}
