package de.bornemisza.users.boundary;

import java.util.UUID;

import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.da.UsersService;
import de.bornemisza.users.entity.User;

public class UsersFacadeTest {

    private UsersService usersService;
    private ITopic<User> newUserAccountTopic, changeEmailRequestTopic;
    private IMap<String, User> newUserAccountMap, changeEmailRequestMap;
    private UsersFacade CUT;
    
    @Before
    public void setUp() {
        usersService = mock(UsersService.class);
        newUserAccountTopic = mock(ITopic.class);
        newUserAccountMap = mock(IMap.class);
        changeEmailRequestTopic = mock(ITopic.class);
        changeEmailRequestMap = mock(IMap.class);
        CUT = new UsersFacade(usersService, newUserAccountTopic, changeEmailRequestTopic, newUserAccountMap, changeEmailRequestMap);
    }

    @Test
    public void addUser() {
        User user = new User();
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
        when(newUserAccountMap.remove(any(String.class))).thenReturn(null);
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
        when(newUserAccountMap.remove(any(String.class))).thenReturn(new User());
        when(usersService.createUser(any(User.class))).thenThrow(new UpdateConflictException());
        assertNull(CUT.confirmUser(UUID.randomUUID().toString()));
    }

    @Test
    public void confirmUser_unauthorized() {
        String msg = "401 - Unauthorized";
        when(newUserAccountMap.remove(any(String.class))).thenReturn(new User());
        when(usersService.createUser(any(User.class))).thenThrow(new DbAccessException(msg));
        try {
            CUT.confirmUser(UUID.randomUUID().toString());
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void confirmUser_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        when(newUserAccountMap.remove(any(String.class))).thenReturn(new User());
        when(usersService.createUser(any(User.class))).thenThrow(new DbAccessException(msg));
        try {
            CUT.confirmUser(UUID.randomUUID().toString());
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void getUser_noSuchUser() {
        when(usersService.getUser(anyString(), any())).thenThrow(new DocumentNotFoundException("/some/path"));
        assertNull(CUT.getUser("Ike", "Basic someAuthString"));
    }

    @Test
    public void getUser_unauthorized() {
        String msg = "401 - Unauthorized";
        when(usersService.getUser(anyString(), any())).thenThrow(new DbAccessException(msg));
        try {
            CUT.getUser("Silly Willy", "Basic someAuthString");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void getUser_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        when(usersService.getUser(anyString(), any())).thenThrow(new DbAccessException(msg));
        try {
            CUT.getUser("Silly Willy", "Basic someAuthString");
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
        when(usersService.changePassword(any(User.class), anyString(), any())).thenThrow(new UpdateConflictException());
        assertNull(CUT.changePassword(new User(), "rev123", "Basic someAuthHeader"));
    }

    @Test
    public void changePassword_unauthorized() {
        String msg = "401 - Unauthorized";
        when(usersService.changePassword(any(User.class), anyString(), any())).thenThrow(new DbAccessException(msg));
        try {
            CUT.changePassword(new User(), "rev123", "Basic someAuthHeader");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void changePassword_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        when(usersService.changePassword(any(User.class), anyString(), any())).thenThrow(new DbAccessException(msg));
        try {
            CUT.changePassword(new User(), "rev123", "Basic someAuthHeader");
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void deleteUser_noSuchUser() {
        doThrow(new UpdateConflictException()).when(usersService).deleteUser(anyString(), anyString(), any());
        assertFalse(CUT.deleteUser("Ike", "3454353", "Basic someAuthString"));
    }

    @Test
    public void deleteUser_unauthorized() {
        String msg = "401 - Unauthorized";
        doThrow(new DbAccessException(msg)).when(usersService).deleteUser(anyString(), anyString(), any());
        try {
            CUT.deleteUser("Silly Willy", "rev123", "Basic someAuthString");
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void deleteUser_TechnicalException() {
        String msg = "java.net.SocketTimeoutException: connect timed out";
        doThrow(new DbAccessException(msg)).when(usersService).deleteUser(anyString(), anyString(), any());
        try {
            CUT.deleteUser("Silly Willy", "rev123", "Basic someAuthString");
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void deleteUser() {
        assertTrue(CUT.deleteUser("Ike", "3454353", "Basic someAuthString"));
    }

}
