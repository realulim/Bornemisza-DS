package de.bornemisza.users.boundary;

import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;

import de.bornemisza.users.da.UsersService;
import de.bornemisza.users.entity.User;

@Stateless
public class UsersFacade {

    @Inject
    UsersService usersService;

    public UsersFacade() { }

    // Constructor for Unit Tests
    public UsersFacade(UsersService usersService) {
        this.usersService = usersService;
    }

    public User createUser(User user, String authHeader) {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.createUser(user);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user + "\n" + e.getMessage());
            return null;
        }
    }

    public User updateUser(User user, String authHeader) {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.updateUser(user);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user + "\n" + e.getMessage());
            return null;
        }
    }

    public User getUser(String userName, String authHeader) {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.getUser(userName);
        }
        catch (DocumentNotFoundException e) {
            return null;
        }
    }

    public boolean deleteUser(String userName, String rev, String authHeader) {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            usersService.deleteUser(userName, rev);
            return true;
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + userName + "\n" + e.getMessage());
            return false;
        }
    }

}
