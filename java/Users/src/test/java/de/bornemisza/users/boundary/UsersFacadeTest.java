package de.bornemisza.users.boundary;


import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;

import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.da.UsersService;
import de.bornemisza.users.entity.User;

public class UsersFacadeTest {

    private UsersService usersService;
    private ITopic<User> newUserAccountTopic;
    private IMap<String, User> newUserAccountMap;
    private UsersFacade CUT;
    
    @Before
    public void setUp() {
        usersService = mock(UsersService.class);
        newUserAccountTopic = mock(ITopic.class);
        newUserAccountMap = mock(IMap.class);
        CUT = new UsersFacade(usersService, newUserAccountTopic, newUserAccountMap);
    }

    @Test
    public void addUser() {
        User user = new User();
        CUT.addUser(user);
        verify(newUserAccountTopic).publish(user);
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
    public void confirmUser_TechnicalException() {
        String msg = "401 - Unauthorized";
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
    public void updateUser_noSuchUser() {
        when(usersService.updateUser(any(User.class), any())).thenThrow(new UpdateConflictException());
        assertNull(CUT.updateUser(new User(), null));
    }

    @Test
    public void updateUser_TechnicalException() {
        String msg = "401 - Unauthorized";
        when(usersService.updateUser(any(User.class), any())).thenThrow(new DbAccessException(msg));
        try {
            CUT.updateUser(new User(), "someAuthString");
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void getUser_noSuchUser() {
        when(usersService.getUser(anyString(), any())).thenThrow(new DocumentNotFoundException("/some/path"));
        assertNull(CUT.getUser("Ike", null));
    }

    @Test
    public void getUser_TechnicalException() {
        String msg = "401 - Unauthorized";
        when(usersService.getUser(anyString(), any())).thenThrow(new DbAccessException(msg));
        try {
            CUT.getUser("Silly Willy", "someAuthString");
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void deleteUser_noSuchUser() {
        doThrow(new UpdateConflictException()).when(usersService).deleteUser(anyString(), anyString(), any());
        assertFalse(CUT.deleteUser("Ike", "3454353", null));
    }

    @Test
    public void deleteUser_TechnicalException() {
        String msg = "401 - Unauthorized";
        doThrow(new DbAccessException(msg)).when(usersService).deleteUser(anyString(), anyString(), any());
        try {
            CUT.deleteUser("Silly Willy", "rev123", "someAuthString");
            fail();
        }
        catch (TechnicalException ex) {
            assertTrue(ex.getMessage().contains(msg));
        }
    }

    @Test
    public void deleteUser() {
        assertTrue(CUT.deleteUser("Ike", "3454353", null));
    }

}
