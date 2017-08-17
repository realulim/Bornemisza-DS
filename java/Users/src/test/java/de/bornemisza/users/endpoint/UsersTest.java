package de.bornemisza.users.endpoint;

import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import de.bornemisza.users.boundary.UsersFacade;
import de.bornemisza.users.entity.User;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class UsersTest {

    private UsersFacade facade;

    private Users CUT;
    
    public UsersTest() {
    }
    
    @Before
    public void setUp() {
        facade = mock(UsersFacade.class);
        CUT = new Users(facade);
    }

    @Test
    public void getUser_technicalException() {
        when(facade.getUser(anyString(), any())).thenThrow(new RuntimeException("Some technical problem..."));
        try {
            CUT.getUser("Ike", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void getUser_userNotFound() {
        when(facade.getUser(anyString(), any())).thenReturn(null);
        try {
            CUT.getUser("Ike", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
        }
    }

    @Test
    public void createUser_technicalException() throws AddressException {
        when(facade.createUser(any(User.class), any())).thenThrow(new RuntimeException("Some technical problem..."));
        User user = new User();
        user.setEmail(new InternetAddress("foo@bar.de"));
        try {
            CUT.createUser(user, null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void createUser_nullUser() {
        try {
            CUT.createUser(null, null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void createUser_nullEmail() {
        try {
            CUT.createUser(new User(), null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void createUser_userAlreadyExists() throws AddressException {
        when(facade.createUser(any(User.class), any())).thenReturn(null);
        User user = new User();
        user.setEmail(new InternetAddress("foo@bar.de"));
        try {
            CUT.createUser(user, null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(409, ex.getResponse().getStatus());
        }
    }

    @Test
    public void updateUser_technicalException() {
        when(facade.updateUser(any(User.class), any())).thenThrow(new RuntimeException("Some technical problem..."));
        try {
            CUT.updateUser(new User(), null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void updateUser_nullUser() {
        try {
            CUT.updateUser(null, null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void updateUser_newerRevisionAlreadyExists() {
        when(facade.updateUser(any(User.class), any())).thenReturn(null);
        try {
            CUT.updateUser(new User(), null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(409, ex.getResponse().getStatus());
        }
    }

    @Test
    public void deleteUser_technicalException() {
        when(facade.deleteUser(anyString(), anyString(), any())).thenThrow(new RuntimeException("Some technical problem..."));
        when(facade.getUser(anyString(), any())).thenReturn(new User());
        try {
            CUT.deleteUser("Ike", "some revision", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void deleteUser_nullUser() {
        when(facade.getUser(anyString(), anyString())).thenReturn(null);
        try {
            CUT.deleteUser("Ike", "some revision", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
        }
    }

    @Test
    public void deleteUser_newerRevisionAlreadyExists() {
        when(facade.getUser(anyString(), any())).thenReturn(new User());
        when(facade.deleteUser(anyString(), anyString(), any())).thenReturn(false);
        try {
            CUT.deleteUser("Ike", "some revision", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(409, ex.getResponse().getStatus());
        }
    }

}
