package de.bornemisza.users.boundary;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import org.ektorp.UpdateConflictException;

import de.bornemisza.users.da.UsersService;
import de.bornemisza.users.entity.User;
import org.ektorp.DocumentNotFoundException;

public class UsersFacadeTest {

    private UsersService usersService;
    private UsersFacade CUT;
    
    @Before
    public void setUp() {
        usersService = mock(UsersService.class);
        CUT = new UsersFacade(usersService);
    }

    @Test
    public void createUser_failed() {
        when(usersService.createUser(any(User.class))).thenThrow(new UpdateConflictException());
        assertNull(CUT.createUser(new User(), null));
    }

    @Test
    public void updateUser_failed() {
        when(usersService.updateUser(any(User.class))).thenThrow(new UpdateConflictException());
        assertNull(CUT.updateUser(new User(), null));
    }

    @Test
    public void getUser_failed() {
        when(usersService.getUser(anyString())).thenThrow(new DocumentNotFoundException("/some/path"));
        assertNull(CUT.getUser("Ike", null));
    }

    @Test
    public void deleteUser_failed() {
        doThrow(new UpdateConflictException()).when(usersService).deleteUser(anyString(), anyString());
        assertFalse(CUT.deleteUser("Ike", "3454353", null));
    }

    @Test
    public void deleteUser() {
        assertTrue(CUT.deleteUser("Ike", "3454353", null));
    }

}
