package de.bornemisza.users.boundary;

import java.util.UUID;

import javax.mail.internet.AddressException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.entity.EmailAddress;
import de.bornemisza.rest.entity.User;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.DocumentNotFoundException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.exception.UpdateConflictException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.da.UsersService;

public class UsersFacadeTest {

    private UsersService usersService;
    private ITopic newUserAccountTopic, changeEmailRequestTopic;
    private IMap newUserAccountMap_userId, newUserAccountMap_uuid, changeEmailRequestMap_userId, changeEmailRequestMap_uuid;
    private HazelcastInstance hazelcast;
    private UsersFacade CUT;
    private final String AUTH_HEADER = "Basic RmF6aWwgT25ndWRhcjpjaGFuZ2Vk";
    private final DoubleSubmitToken dsToken = new DoubleSubmitToken(
            "AuthSession=b866f6e2-be02-4ea0-99e6-34f989629930; Version=1; Path=/; HttpOnly; Secure", 
            "dc09ed95c35268bd29798bfa2ac6ee0142d8e1030475663e1ea4db8cb1f20f0b");

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
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn("My Super Password".toCharArray());
        CUT = new UsersFacade(usersService, hazelcast, lbConfig);
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
            assertEquals(UsersType.USER_ALREADY_EXISTS, be.getType());
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
            assertEquals(UsersType.EMAIL_ALREADY_EXISTS, be.getType());
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
            assertEquals(UsersType.UUID_NOT_FOUND, e.getType());
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
    public void confirmUser() throws AddressException {
        char[] password = new char[] {'s', 'r', 'm', 't'};
        User user = new User();
        user.setName("Fridolin");
        user.setEmail(new EmailAddress("foo@bar.de"));
        user.setPassword(password);
        User user2 = new User();
        user2.setEmail(user.getEmail());
        user2.setName(user.getName());
        user2.setPassword(null); // CouchDB does this
        when(newUserAccountMap_uuid.remove(any(String.class))).thenReturn(user);
        when(usersService.createUser(any(User.class))).thenReturn(user2);

        User createdUser = CUT.confirmUser(UUID.randomUUID().toString());
        assertNotNull(createdUser);
        assertNull(createdUser.getPassword());
    }

    @Test
    public void confirmEmail_uuidNotFound() {
        when(newUserAccountMap_uuid.remove(any(String.class))).thenReturn(null);
        try {
            CUT.confirmEmail(UUID.randomUUID().toString(), null);
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UUID_NOT_FOUND, e.getType());
        }
    }

    @Test
    public void confirmEmail_nonExistingUser() {
        when(changeEmailRequestMap_uuid.remove(any(String.class))).thenReturn(new User());
        when(usersService.getUser(any(Auth.class), anyString())).thenReturn(null);
        try {
            CUT.confirmEmail(UUID.randomUUID().toString(), AUTH_HEADER);
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.USER_NOT_FOUND, e.getType());
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
        when(usersService.getUser(any(Auth.class), anyString())).thenReturn(new User());
        when(usersService.existsEmail(any(EmailAddress.class))).thenReturn(false);
        when(usersService.updateUser(any(Auth.class), any(User.class))).thenThrow(new TechnicalException(msg));
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
        when(usersService.getUser(any(Auth.class), anyString())).thenReturn(new User());
        when(usersService.existsEmail(any(EmailAddress.class))).thenReturn(true);
        assertNull(CUT.confirmEmail(UUID.randomUUID().toString(), AUTH_HEADER));
    }

    @Test
    public void confirmEmail_updateConflict_newerRevisionExists() {
        User user = new User();
        user.setName("Ike");
        when(changeEmailRequestMap_uuid.remove(any(String.class))).thenReturn(user);
        when(usersService.getUser(any(Auth.class), anyString())).thenReturn(new User());
        when(usersService.existsEmail(any(EmailAddress.class))).thenReturn(false);
        when(usersService.updateUser(any(Auth.class), any(User.class))).thenThrow(new UpdateConflictException(""));
        assertNull(CUT.confirmEmail(UUID.randomUUID().toString(), AUTH_HEADER));
    }

    @Test
    public void getUser_noSuchUser() {
        when(usersService.getUser(any(Auth.class), anyString())).thenThrow(new DocumentNotFoundException("/some/path"));
        assertNull(CUT.getUser("Ike", AUTH_HEADER));
        assertNull(CUT.getUser("Ike", dsToken));
    }

    @Test
    public void getUser_unauthorized_usernamePasswordScheme() {
        try {
            CUT.getUser("Silly Willy", "BrokenAuthHeader");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().startsWith("401 AuthHeader"));
        }
    }

    @Test
    public void getUser_unauthorized_cookieTokenScheme() {
        try {
            CUT.getUser("Silly Willy", new DoubleSubmitToken("Cookie", "notReallyTheHashedCookie"));
            fail();
        }
        catch (UnauthorizedException ex) {
            assertEquals("Hash Mismatch!", ex.getMessage());
        }
    }

    @Test
    public void getUser_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        when(usersService.getUser(any(Auth.class), anyString())).thenThrow(new TechnicalException(msg));
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
        when(usersService.changePassword(any(Auth.class), any(User.class))).thenThrow(new UpdateConflictException(""));
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
        when(usersService.changePassword(any(Auth.class), any(User.class))).thenThrow(new TechnicalException(msg));
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
        doThrow(new UpdateConflictException("")).when(usersService).deleteUser(any(Auth.class), anyString(), anyString());
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
        doThrow(new TechnicalException(msg)).when(usersService).deleteUser(any(Auth.class), anyString(), anyString());
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
