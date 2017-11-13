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
import de.bornemisza.couchdb.entity.User;

import de.bornemisza.users.boundary.BusinessException;
import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.boundary.UnauthorizedException;
import de.bornemisza.users.boundary.UsersFacade;

public class UsersTest {

    private UsersFacade facade;

    private Users CUT;
    
    private static final String AUTH_HEADER = "Basic someAuthString";
    private static final String SOCKET_TIMEOUT = "java.net.SocketTimeoutException: connect timed out";

    public UsersTest() {
    }
    
    @Before
    public void setUp() {
        facade = mock(UsersFacade.class);
        CUT = new Users(facade);
    }

    @Test
    public void getUser_userNameVoid() {
        try {
            CUT.getUser("null", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void getUser_illegalCharactersInUserName() {
        try {
            CUT.getUser("<script>alert('hooboo');</script>", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void getUser_technicalException() {
        when(facade.getUser(anyString(), any())).thenThrow(new RuntimeException(SOCKET_TIMEOUT));
        try {
            CUT.getUser("Ike", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals(SOCKET_TIMEOUT, ex.getResponse().getEntity());
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
    public void userAccountCreationRequest_nullUser() {
        try {
            CUT.userAccountCreationRequest(null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void userAccountCreationRequest_nullEmail() {
        try {
            CUT.userAccountCreationRequest(new User());
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void userAccountCreationRequest_illegalCharactersInUserName() {
        try {
            User user = new User();
            user.setName("<");
            user.setEmail(new InternetAddress());
            CUT.userAccountCreationRequest(user);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void userAccountCreationRequest_technicalException() throws AddressException {
        User user = new User();
        user.setEmail(new InternetAddress("foo@bar.de"));
        doThrow(new TopicOverloadException("Topic overloaded")).when(facade).addUser(user);
        try {
            CUT.userAccountCreationRequest(user);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void userAccountCreationRequest_userAlreadyExists() throws AddressException {
        User user = new User();
        user.setName("Ike");
        user.setEmail(new InternetAddress("foo@bar.de"));
        doThrow(new BusinessException(Type.USER_ALREADY_EXISTS, user.getName())).when(facade).addUser(user);
        try {
            CUT.userAccountCreationRequest(user);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(409, e.getResponse().getStatus());
        }
    }

    @Test
    public void userAccountCreationRequest_unknownBusinessException() throws AddressException {
        User user = new User();
        user.setName("Ike");
        user.setEmail(new InternetAddress("foo@bar.de"));
        doThrow(new BusinessException(Type.UUID_NOT_FOUND, user.getName())).when(facade).addUser(user);
        try {
            CUT.userAccountCreationRequest(user);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(500, e.getResponse().getStatus());
        }
    }

    @Test
    public void userAccountCreationRequest() throws AddressException {
        User user = new User();
        user.setName("Ike");
        user.setEmail(new InternetAddress("foo@bar.de"));
        Response response = CUT.userAccountCreationRequest(user);
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
            assertEquals("UUID missing or unparseable!", ex.getResponse().getEntity().toString());
        }
    }

    @Test
    public void confirmUser_requestExpired() {
        String uuid = UUID.randomUUID().toString();
        String errMsg = "User Account Creation Request does not exist - maybe expired?";
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
        when(facade.confirmUser(uuid)).thenThrow(new RuntimeException(SOCKET_TIMEOUT));
        try {
            CUT.confirmUser(uuid);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals(SOCKET_TIMEOUT, ex.getResponse().getEntity());
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
    public void changeEmailRequest_userNameVoid() {
        try {
            CUT.changeEmailRequest("", "foo@bar.de", AUTH_HEADER);
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void changeEmailRequest_nullEmail() {
        try {
            CUT.changeEmailRequest("Ike", "null", AUTH_HEADER);
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
            assertEquals("E-Mail missing or unparseable!", ex.getResponse().getEntity().toString());
        }
    }

    @Test
    public void changeEmailRequest_unparseableEmail() {
        try {
            CUT.changeEmailRequest("Ike", "foobar@foo@.de", AUTH_HEADER);
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
            assertEquals("E-Mail missing or unparseable!", ex.getResponse().getEntity().toString());
        }
    }

    @Test
    public void changeEmailRequest_technicalException() throws AddressException {
        String emailStr = "foo@bar.de";
        User user = new User();
        user.setName("Ike");
        user.setEmail(new InternetAddress(emailStr));
        when(facade.getUser(anyString(), any())).thenReturn(user);
        doThrow(new TopicOverloadException("Topic overloaded")).when(facade).changeEmail(user);
        try {
            CUT.changeEmailRequest(user.getName(), emailStr, AUTH_HEADER);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
        }
    }

    @Test
    public void changeEmailRequest() throws AddressException {
        String emailStr = "foo@bar.de";
        User user = new User();
        user.setName("Ike");
        user.setEmail(new InternetAddress(emailStr));
        when(facade.getUser(anyString(), any())).thenReturn(user);
        Response response = CUT.changeEmailRequest(user.getName(), emailStr, AUTH_HEADER);
        assertEquals(202, response.getStatus());
    }

    @Test
    public void confirmEmail_requestExpired() {
        String uuid = UUID.randomUUID().toString();
        String errMsg = "E-Mail Change Request does not exist - maybe expired?";
        when(facade.confirmEmail(uuid, AUTH_HEADER)).thenThrow(new BusinessException(Type.UUID_NOT_FOUND, errMsg));
        try {
            CUT.confirmEmail(uuid, AUTH_HEADER);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
            assertEquals(errMsg, ex.getResponse().getEntity());
        }
    }

    @Test
    public void confirmEmail_newerRevisionExists() throws AddressException {
        String uuid = UUID.randomUUID().toString();
        when(facade.confirmEmail(uuid, AUTH_HEADER)).thenReturn(null);
        try {
            CUT.confirmEmail(uuid, AUTH_HEADER);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(409, ex.getResponse().getStatus());
        }
    }

    @Test
    public void changePassword_userNameVoid() {
        try {
            CUT.changePassword("", "newPassword", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void changePassword_passwordVoid() {
        try {
            CUT.changePassword("Ike", null, null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void changePassword_technicalException() {
        User user = new User();
        user.setRevision("rev123");
        when(facade.getUser(anyString(), any())).thenReturn(user);
        when(facade.changePassword(any(User.class), anyString(), any())).thenThrow(new RuntimeException(SOCKET_TIMEOUT));
        try {
            CUT.changePassword("Ike", "newPassword", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals(SOCKET_TIMEOUT, ex.getResponse().getEntity());
        }
    }

    @Test
    public void changePassword_newerRevisionAlreadyExists() {
        when(facade.getUser(anyString(), any())).thenReturn(new User());
        when(facade.changePassword(any(User.class), anyString(), any())).thenReturn(null);
        try {
            CUT.changePassword("Ike", "newPassword", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(409, ex.getResponse().getStatus());
            assertEquals("Newer Revision exists!", ex.getResponse().getEntity().toString());
        }
    }

    @Test
    public void deleteUser_nameVoid() {
        try {
            CUT.deleteUser("null", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
        }
    }

    @Test
    public void deleteUser_technicalException() {
        User user = new User();
        user.setRevision("rev123");
        when(facade.deleteUser(anyString(), anyString(), any())).thenThrow(new RuntimeException(SOCKET_TIMEOUT));
        when(facade.getUser(anyString(), any())).thenReturn(user);
        try {
            CUT.deleteUser("Ike", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals(SOCKET_TIMEOUT, ex.getResponse().getEntity());
        }
    }

    @Test
    public void deleteUser_newerRevisionAlreadyExists() {
        when(facade.getUser(anyString(), any())).thenReturn(new User());
        when(facade.deleteUser(anyString(), anyString(), any())).thenReturn(false);
        try {
            CUT.deleteUser("Ike", null);
            fail();
        }
        catch (WebApplicationException ex) {
            assertEquals(409, ex.getResponse().getStatus());
            assertEquals("Newer Revision exists!", ex.getResponse().getEntity().toString());
        }
    }

}
