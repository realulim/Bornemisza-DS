package de.bornemisza.users.boundary;

import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.ektorp.DbAccessException;
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

    public User createUser(User user) {
        try {
            return usersService.createUser(user);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user + "\n" + e.getMessage());
            return null;
        }
        catch (DbAccessException ex) {
            throw new TechnicalException(ex.toString());
        }
    }

    public User updateUser(User user, String authHeader) {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.updateUser(user, creds);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user + "\n" + e.getMessage());
            return null;
        }
        catch (DbAccessException ex) {
            throw new TechnicalException(ex.toString());
        }
    }

    public User getUser(String userName, String authHeader) {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.getUser(userName, creds);
        }
        catch (DocumentNotFoundException e) {
            return null;
        }
        catch (DbAccessException ex) {
            throw new TechnicalException(ex.toString());
        }
    }

    public boolean deleteUser(String userName, String rev, String authHeader) {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            usersService.deleteUser(userName, rev, creds);
            return true;
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + userName + "\n" + e.getMessage());
            return false;
        }
        catch (DbAccessException ex) {
            throw new TechnicalException(ex.toString());
        }
    }

}
