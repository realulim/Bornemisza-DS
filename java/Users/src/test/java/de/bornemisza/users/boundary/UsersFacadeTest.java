package de.bornemisza.users.boundary;

import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.users.da.UsersService;
import de.bornemisza.users.entity.User;

public class UsersFacadeTest {

    private UsersService usersService;
    private UsersFacade CUT;
    
    @Before
    public void setUp() {
        usersService = mock(UsersService.class);
        CUT = new UsersFacade(usersService);
    }

    @Test
    public void createUser_userExists() {
        when(usersService.createUser(any(User.class))).thenThrow(new UpdateConflictException());
        assertNull(CUT.createUser(new User()));
    }

    @Test
    public void createUser_TechnicalException() {
        String msg = "401 - Unauthorized";
        when(usersService.createUser(any(User.class))).thenThrow(new DbAccessException(msg));
        try {
            CUT.createUser(new User());
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
