package de.bornemisza.users.boundary;

import java.util.UUID;

import javax.mail.internet.AddressException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.rest.entity.EmailAddress;
import de.bornemisza.rest.entity.User;
import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.da.UsersService;

public class UsersFacadeTest {

    private UsersService usersService;
    private ITopic newUserAccountTopic, changeEmailRequestTopic;
    private IMap newUserAccountMap_userId, newUserAccountMap_uuid, changeEmailRequestMap_userId, changeEmailRequestMap_uuid;
    private HazelcastInstance hazelcast;
    private UsersFacade CUT;
    private final String AUTH_HEADER = "Basic RmF6aWwgT25ndWRhcjpjaGFuZ2Vk";

    @Before
    public void setUp() {
        usersService = mock(UsersService.class);
        newUserAccountTopic = mock(ITopic.class);
        newUserAccountMap_userId = mock(IMap.class);
        newUserAccountMap_uuid = mock(IMap.class);
        changeEmailRequestTopic = mock(ITopic.class);
        changeEmailRequestMap_userId = mock(IMap.class);
        changeEmailRequestMap_uuid = mock(IMap.class);
        hazelcast = mock(HazelcastInstance.class);
        when(hazelcast.getMap(eq(JAXRSConfiguration.MAP_NEW_USER_ACCOUNT + JAXRSConfiguration.MAP_USERID_SUFFIX))).thenReturn(newUserAccountMap_userId);
        when(hazelcast.getMap(eq(JAXRSConfiguration.MAP_NEW_USER_ACCOUNT + JAXRSConfiguration.MAP_UUID_SUFFIX))).thenReturn(newUserAccountMap_uuid);
        when(hazelcast.getMap(eq(JAXRSConfiguration.MAP_CHANGE_EMAIL_REQUEST + JAXRSConfiguration.MAP_USERID_SUFFIX))).thenReturn(changeEmailRequestMap_userId);
        when(hazelcast.getMap(eq(JAXRSConfiguration.MAP_CHANGE_EMAIL_REQUEST + JAXRSConfiguration.MAP_UUID_SUFFIX))).thenReturn(changeEmailRequestMap_uuid);
        when(hazelcast.getTopic(JAXRSConfiguration.TOPIC_NEW_USER_ACCOUNT)).thenReturn(newUserAccountTopic);
        when(hazelcast.getTopic(JAXRSConfiguration.TOPIC_CHANGE_EMAIL_REQUEST)).thenReturn(changeEmailRequestTopic);
        CUT = new UsersFacade(usersService, hazelcast);
    }

    @Test
    public void addUser_userAlreadyExists() {
        User user = new User();
        user.setName("Ike");
        when(usersService.existsUser(user.getName())).thenReturn(true);
        try {
            CUT.addUser(user);
            fail();
        }
        catch (BusinessException be) {
            assertEquals(BusinessException.Type.USER_ALREADY_EXISTS, be.getType());
        }
    }

    @Test
    public void addUser_emailAlreadyExists() throws AddressException {
        User user = new User();
        user.setName("Ike");
        user.setEmail(new EmailAddress("foo@bar.de"));
        when(usersService.existsUser(user.getName())).thenReturn(false);
        when(usersService.existsEmail(user.getEmail())).thenReturn(true);
        try {
            CUT.addUser(user);
            fail();
        }
        catch (BusinessException be) {
            assertEquals(BusinessException.Type.EMAIL_ALREADY_EXISTS, be.getType());
        }
    }

    @Test
    public void addUser() {
        User user = new User();
        user.setName("Ike");
        when(usersService.existsUser(user.getName())).thenReturn(false);
        CUT.addUser(user);
        verify(newUserAccountTopic).publish(user);
    }

    @Test
    public void changeEmail() {
        User user = new User();
        CUT.changeEmail(user);
        verify(changeEmailRequestTopic).publish(user);
    }

    @Test
    public void confirmUser_uuidNotFound() {
        when(newUserAccountMap_uuid.remove(any(String.class))).thenReturn(null);
        try {
            CUT.confirmUser(UUID.randomUUID().toString());
            fail();
        }
        catch (BusinessException e) {
            assertEquals(Type.UUID_NOT_FOUND, e.getType());
        }
    }

    @Test
    public void confirmUser_userExists() {
        when(newUserAccountMap_uuid.remove(any(String.class))).thenReturn(new User());
        when(usersService.createUser(any(User.class))).thenThrow(new UpdateConflictException(""));
        assertNull(CUT.confirmUser(UUID.randomUUID().toString()));
    }

    @Test
    public void confirmUser_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        when(newUserAccountMap_uuid.remove(any(String.class))).thenReturn(new User());
        when(usersService.createUser(any(User.class))).thenThrow(new TechnicalException(msg));
        try {
            CUT.confirmUser(UUID.randomUUID().toString());
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void confirmEmail_uuidNotFound() {
        when(newUserAccountMap_uuid.remove(any(String.class))).thenReturn(null);
        try {
            CUT.confirmEmail(UUID.randomUUID().toString(), null);
            fail();
        }
        catch (BusinessException e) {
            assertEquals(Type.UUID_NOT_FOUND, e.getType());
        }
    }

    @Test
    public void confirmEmail_nonExistingUser() {
        when(changeEmailRequestMap_uuid.remove(any(String.class))).thenReturn(new User());
        when(usersService.getUser(anyString(), any())).thenReturn(null);
        try {
            CUT.confirmEmail(UUID.randomUUID().toString(), AUTH_HEADER);
            fail();
        }
        catch (BusinessException e) {
            assertEquals(Type.USER_NOT_FOUND, e.getType());
        }
    }

    @Test
    public void confirmEmail_unauthorized() {
        User user = new User();
        user.setName("Ike");
        when(changeEmailRequestMap_uuid.remove(any(String.class))).thenReturn(user);
        try {
            CUT.confirmEmail(UUID.randomUUID().toString(), "BrokenAuthHeader");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().startsWith("401 AuthHeader"));
        }
    }

    @Test
    public void confirmEmail_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        User user = new User();
        user.setName("Ike");
        when(changeEmailRequestMap_uuid.remove(any(String.class))).thenReturn(user);
        when(usersService.getUser(anyString(), any())).thenReturn(new User());
        when(usersService.existsEmail(any(EmailAddress.class))).thenReturn(false);
        when(usersService.updateUser(any(User.class), any())).thenThrow(new TechnicalException(msg));
        try {
            CUT.confirmEmail(UUID.randomUUID().toString(), AUTH_HEADER);
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void confirmEmail_updateConflict_emailAlreadyExists() throws AddressException {
        User user = new User();
        user.setName("Ike");
        user.setEmail(new EmailAddress("foo@bar.de"));
        when(changeEmailRequestMap_uuid.remove(any(String.class))).thenReturn(user);
        when(usersService.getUser(anyString(), any())).thenReturn(new User());
        when(usersService.existsEmail(any(EmailAddress.class))).thenReturn(true);
        assertNull(CUT.confirmEmail(UUID.randomUUID().toString(), AUTH_HEADER));
    }

    @Test
    public void confirmEmail_updateConflict_newerRevisionExists() {
        User user = new User();
        user.setName("Ike");
        when(changeEmailRequestMap_uuid.remove(any(String.class))).thenReturn(user);
        when(usersService.getUser(anyString(), any())).thenReturn(new User());
        when(usersService.existsEmail(any(EmailAddress.class))).thenReturn(false);
        when(usersService.updateUser(any(User.class), any())).thenThrow(new UpdateConflictException(""));
        assertNull(CUT.confirmEmail(UUID.randomUUID().toString(), AUTH_HEADER));
    }

    @Test
    public void getUser_noSuchUser() {
        when(usersService.getUser(anyString(), any())).thenThrow(new DocumentNotFoundException("/some/path"));
        assertNull(CUT.getUser("Ike", AUTH_HEADER));
    }

    @Test
    public void getUser_unauthorized() {
        try {
            CUT.getUser("Silly Willy", "BrokenAuthHeader");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().startsWith("401 AuthHeader"));
        }
    }

    @Test
    public void getUser_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        when(usersService.getUser(anyString(), any())).thenThrow(new TechnicalException(msg));
        try {
            CUT.getUser("Silly Willy", AUTH_HEADER);
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void changePassword_updateConflict() {
        User user = new User();
        user.setName("Bogumil");
        when(usersService.changePassword(any(User.class), any())).thenThrow(new UpdateConflictException(""));
        assertNull(CUT.changePassword(new User(), "rev123", AUTH_HEADER));
    }

    @Test
    public void changePassword_unauthorized() {
        try {
            CUT.changePassword(new User(), "rev123", "BrokenAuthHeader");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().startsWith("401 AuthHeader"));
        }
    }

    @Test
    public void changePassword_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        when(usersService.changePassword(any(User.class), any())).thenThrow(new TechnicalException(msg));
        try {
            CUT.changePassword(new User(), "rev123", AUTH_HEADER);
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void deleteUser_noSuchUser() {
        doThrow(new UpdateConflictException("")).when(usersService).deleteUser(anyString(), anyString(), any());
        assertFalse(CUT.deleteUser("Ike", "3454353", AUTH_HEADER));
    }

    @Test
    public void deleteUser_unauthorized() {
        try {
            CUT.deleteUser("Silly Willy", "rev123", "BrokenAuthHeader");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().startsWith("401 AuthHeader"));
        }
    }

    @Test
    public void deleteUser_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        doThrow(new TechnicalException(msg)).when(usersService).deleteUser(anyString(), anyString(), any());
        try {
            CUT.deleteUser("Silly Willy", "rev123", AUTH_HEADER);
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void deleteUser() {
        assertTrue(CUT.deleteUser("Ike", "3454353", AUTH_HEADER));
    }

}
