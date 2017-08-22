package de.bornemisza.users.boundary;

import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.da.UsersService;
import de.bornemisza.users.entity.User;

@Stateless
public class UsersFacade {

    @Inject
    UsersService usersService;

    @Inject
    HazelcastInstance hazelcast;

    private ITopic<User> newUserAccountTopic;
    private IMap<UUID, User> newUserAccountMap;

    public UsersFacade() { }

    // Constructor for Unit Tests
    public UsersFacade(UsersService usersService, ITopic<User> newUserAccountTopic, IMap<UUID, User> newUserAccountMap) {
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

    public User confirmUser(UUID uuid) {
        User user = newUserAccountMap.remove(uuid);
        if (user == null) throw new NotFoundException(uuid.toString());
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

