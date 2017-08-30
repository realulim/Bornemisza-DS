package de.bornemisza.users.boundary;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.auth.login.CredentialNotFoundException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;

import de.bornemisza.rest.BasicAuthCredentials;
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

    private ITopic<User> newUserAccountTopic, changeEmailRequestTopic;
    private IMap<String, User> newUserAccountMap, changeEmailRequestMap;

    public UsersFacade() { }

    // Constructor for Unit Tests
    public UsersFacade(UsersService usersService, ITopic<User> newUserAccountTopic, ITopic<User> changeEmailRequestTopic, IMap<String, User> newUserAccountMap, IMap<String, User> changeEmailRequestMap) {
        this.usersService = usersService;
        this.newUserAccountTopic = newUserAccountTopic;
        this.changeEmailRequestTopic = changeEmailRequestTopic;
        this.newUserAccountMap = newUserAccountMap;
        this.changeEmailRequestMap = changeEmailRequestMap;
    }

    @PostConstruct
    public void init() {
        this.newUserAccountTopic = hazelcast.getTopic(JAXRSConfiguration.TOPIC_NEW_USER_ACCOUNT);
        this.newUserAccountMap = hazelcast.getMap(JAXRSConfiguration.MAP_NEW_USER_ACCOUNT);
        this.changeEmailRequestTopic = hazelcast.getTopic(JAXRSConfiguration.TOPIC_CHANGE_EMAIL_REQUEST);
        this.changeEmailRequestMap = hazelcast.getMap(JAXRSConfiguration.MAP_CHANGE_EMAIL_REQUEST);
    }

    public void addUser(User user) {
        if (usersService.existsUser(user.getName())) {
            throw new BusinessException(Type.USER_ALREADY_EXISTS, user.getName() + " already exists!");
        }
        else if (usersService.existsEmail(user.getEmail())) {
            throw new BusinessException(Type.EMAIL_ALREADY_EXISTS, user.getEmail().getAddress() + " already exists!");
        }
        else {
            newUserAccountTopic.publish(user);
        }
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
            if (ex.getMessage().startsWith("401")) throw new UnauthorizedException(ex.getMessage());
            else throw new TechnicalException(ex.toString());
        }
    }

    public void changeEmail(User user) {
        changeEmailRequestTopic.publish(user);
    }

    public User confirmEmail(String uuid, String authHeader) throws BusinessException, TechnicalException, UnauthorizedException {
        User user = changeEmailRequestMap.remove(uuid);
        if (user == null) {
            throw new BusinessException(Type.UUID_NOT_FOUND, uuid);
        }
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            User newUser = usersService.getUser(user.getName(), creds);
            if (newUser == null) throw new BusinessException(Type.USER_NOT_FOUND, user.getName());
            newUser.setEmail(user.getEmail());
            newUser.setPassword(creds.getPassword().toCharArray()); // otherwise password will be reset by CouchDB
            if (usersService.existsEmail(newUser.getEmail())) {
                // Someone else has in the meantime acquired this E-Mail address
                Logger.getAnonymousLogger().warning("Update Conflict: " + newUser.getEmail().getAddress() + " already exists!");
                return null;
            }
            return usersService.updateUser(newUser, creds);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user + "\n" + e.getMessage());
            return null;
        }
        catch (DbAccessException | CredentialNotFoundException ex) {
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
        catch (DbAccessException | CredentialNotFoundException ex) {
            if (ex.getMessage().startsWith("401")) throw new UnauthorizedException(ex.getMessage());
            else throw new TechnicalException(ex.toString());
        }
    }

    public User changePassword(User user, String rev, String authHeader) throws UnauthorizedException, TechnicalException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.changePassword(user, rev, creds);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user.getName() + "\n" + e.getMessage());
            return null;
        }
        catch (DbAccessException | CredentialNotFoundException ex) {
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
        catch (DbAccessException | CredentialNotFoundException ex) {
            if (ex.getMessage().startsWith("401")) throw new UnauthorizedException(ex.getMessage());
            else throw new TechnicalException(ex.toString());
        }
    }

}
