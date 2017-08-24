package de.bornemisza.users.boundary;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;

import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.da.UsersService;
import de.bornemisza.users.entity.User;

@Stateless
public class UsersFacade {

    @Inject
    UsersService usersService;

    @Inject
    HazelcastInstance hazelcast;

    private ITopic<User> newUserAccountTopic;
    private IMap<String, User> newUserAccountMap;

    public UsersFacade() { }

    // Constructor for Unit Tests
    public UsersFacade(UsersService usersService, ITopic<User> newUserAccountTopic, IMap<String, User> newUserAccountMap) {
        this.usersService = usersService;
        this.newUserAccountTopic = newUserAccountTopic;
        this.newUserAccountMap = newUserAccountMap;
    }

    @PostConstruct
    public void init() {
        this.newUserAccountTopic = hazelcast.getTopic(JAXRSConfiguration.TOPIC_NEW_USER_ACCOUNT);
        this.newUserAccountMap = hazelcast.getMap(JAXRSConfiguration.MAP_NEW_USER_ACCOUNT);
    }

    public void addUser(User user) {
        newUserAccountTopic.publish(user);
    }

    public User confirmUser(String uuid) throws BusinessException, TechnicalException {
        User user = newUserAccountMap.remove(uuid);
        if (user == null) {
            throw new BusinessException(Type.UUID_NOT_FOUND, uuid);
        }
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

    public User updateUser(User user, String authHeader) throws UnauthorizedException, TechnicalException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.updateUser(user, creds);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user + "\n" + e.getMessage());
            return null;
        }
        catch (DbAccessException ex) {
            if (ex.getMessage().startsWith("401")) throw new UnauthorizedException(ex.getMessage());
            else throw new TechnicalException(ex.toString());
        }
    }

    public User getUser(String userName, String authHeader) throws UnauthorizedException, TechnicalException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.getUser(userName, creds);
        }
        catch (DocumentNotFoundException e) {
            return null;
        }
        catch (DbAccessException ex) {
            if (ex.getMessage().startsWith("401")) throw new UnauthorizedException(ex.getMessage());
            else throw new TechnicalException(ex.toString());
        }
    }

    public boolean deleteUser(String userName, String rev, String authHeader) throws UnauthorizedException, TechnicalException {
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
            if (ex.getMessage().startsWith("401")) throw new UnauthorizedException(ex.getMessage());
            else throw new TechnicalException(ex.toString());
        }
    }

}

