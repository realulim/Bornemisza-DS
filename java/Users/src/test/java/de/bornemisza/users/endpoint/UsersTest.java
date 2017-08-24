package de.bornemisza.users.endpoint;

import java.util.UUID;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import com.hazelcast.topic.TopicOverloadException;
import de.bornemisza.users.boundary.BusinessException;
import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.boundary.UnauthorizedException;

import de.bornemisza.users.boundary.UsersFacade;
import de.bornemisza.users.entity.User;

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
    public void getUser_unauthorized() {
        when(facade.getUser(anyString(), any())).thenThrow(new UnauthorizedException("401"));
        try {
            CUT.getUser("Ike", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(401, ex.getResponse().getStatus());
        }
    }

    @Test
    public void addUser_nullUser() {
        try {
            CUT.addUser(null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void addUser_nullEmail() {
        try {
            CUT.addUser(new User());
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void addUser_technicalException() throws AddressException {
        User user = new User();
        user.setEmail(new InternetAddress("foo@bar.de"));
        doThrow(new TopicOverloadException("Topic overloaded")).when(facade).addUser(user);
        try {
            CUT.addUser(user);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void addUser() throws AddressException {
        User user = new User();
        user.setEmail(new InternetAddress("foo@bar.de"));
        Response response = CUT.addUser(user);
        assertEquals(202, response.getStatus());
    }

    @Test
    public void confirmUser_nullUuid() {
        try {
            CUT.confirmUser(null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void confirmUser_unparseableUuid() {
        try {
            CUT.confirmUser("this-is-not-a-uuid");
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void confirmUser_userExpired() {
        String uuid = UUID.randomUUID().toString();
        String errMsg = "User does not exist - maybe expired?";
        when(facade.confirmUser(uuid)).thenThrow(new BusinessException(Type.UUID_NOT_FOUND, errMsg));
        try {
            CUT.confirmUser(uuid);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
            assertEquals(errMsg, ex.getResponse().getEntity());
        }
    }

    @Test
    public void confirmUser_technicalException() throws AddressException {
        String uuid = UUID.randomUUID().toString();
        when(facade.confirmUser(uuid)).thenThrow(new RuntimeException("Some technical problem..."));
        try {
            CUT.confirmUser(uuid);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void confirmUser_userAlreadyExists() throws AddressException {
        String uuid = UUID.randomUUID().toString();
        when(facade.confirmUser(uuid)).thenReturn(null);
        try {
            CUT.confirmUser(uuid);
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
    public void updateUser_unauthorized() {
        when(facade.updateUser(any(User.class), any())).thenThrow(new UnauthorizedException("401"));
        try {
            CUT.updateUser(new User(), null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(401, ex.getResponse().getStatus());
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
    public void deleteUser_nullRevision() {
        try {
            CUT.deleteUser("Ike", "null", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
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

    @Test
    public void deleteUser_unauthorized() {
        when(facade.getUser(anyString(), any())).thenThrow(new UnauthorizedException("401"));
        try {
            CUT.deleteUser("Ike", "some revision", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(401, ex.getResponse().getStatus());
        }
    }

}
