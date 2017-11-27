package de.bornemisza.users.boundary;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.auth.login.CredentialNotFoundException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.rest.entity.User;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.DocumentNotFoundException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.exception.UpdateConflictException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.da.UsersService;

@Stateless
public class UsersFacade {

    @Inject
    UsersService usersService;

    @Inject
    HazelcastInstance hazelcast;

    private ITopic<User> newUserAccountTopic, changeEmailRequestTopic;
    private IMap<String, User> newUserAccountMap_userId, newUserAccountMap_uuid, changeEmailRequestMap_userId, changeEmailRequestMap_uuid;

    public UsersFacade() { }

    // Constructor for Unit Tests
    public UsersFacade(UsersService usersService, HazelcastInstance hz) {
        this.usersService = usersService;
        this.hazelcast = hz;
        init();
    }

    @PostConstruct
    public final void init() {
        this.newUserAccountTopic = hazelcast.getTopic(JAXRSConfiguration.TOPIC_NEW_USER_ACCOUNT);
        this.newUserAccountMap_userId = hazelcast.getMap(JAXRSConfiguration.MAP_NEW_USER_ACCOUNT + JAXRSConfiguration.MAP_USERID_SUFFIX);
        this.newUserAccountMap_uuid = hazelcast.getMap(JAXRSConfiguration.MAP_NEW_USER_ACCOUNT + JAXRSConfiguration.MAP_UUID_SUFFIX);
        this.changeEmailRequestTopic = hazelcast.getTopic(JAXRSConfiguration.TOPIC_CHANGE_EMAIL_REQUEST);
        this.changeEmailRequestMap_userId = hazelcast.getMap(JAXRSConfiguration.MAP_CHANGE_EMAIL_REQUEST + JAXRSConfiguration.MAP_USERID_SUFFIX);
        this.changeEmailRequestMap_uuid = hazelcast.getMap(JAXRSConfiguration.MAP_CHANGE_EMAIL_REQUEST + JAXRSConfiguration.MAP_UUID_SUFFIX);
    }

    public void addUser(User user) throws BusinessException, TechnicalException {
        if (usersService.existsUser(user.getName())) {
            throw new BusinessException(UsersType.USER_ALREADY_EXISTS, user.getName() + " already exists!");
        }
        else if (usersService.existsEmail(user.getEmail())) {
            throw new BusinessException(UsersType.EMAIL_ALREADY_EXISTS, user.getEmail().getAddress() + " already exists!");
        }
        else {
            newUserAccountTopic.publish(user);
        }
    }

    public User confirmUser(String uuid) throws BusinessException, TechnicalException {
        User user = newUserAccountMap_uuid.remove(uuid);
        if (user == null) {
            throw new BusinessException(UsersType.UUID_NOT_FOUND, uuid);
        }
        newUserAccountMap_userId.delete(user.getId());
        try {
            return usersService.createUser(user);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user + "\n" + e.getMessage());
            return null;
        }
    }

    public void changeEmail(User user) {
        changeEmailRequestTopic.publish(user);
    }

    public User confirmEmail(String uuid, String authHeader) throws BusinessException, TechnicalException, UnauthorizedException {
        User user = changeEmailRequestMap_uuid.remove(uuid);
        if (user == null) {
            throw new BusinessException(UsersType.UUID_NOT_FOUND, uuid);
        }
        changeEmailRequestMap_userId.delete(user.getId());
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            Auth auth = new Auth(creds);
            User newUser = usersService.getUser(auth, user.getName());
            if (newUser == null) throw new BusinessException(UsersType.USER_NOT_FOUND, user.getName());
            newUser.setEmail(user.getEmail());
            newUser.setPassword(creds.getPassword().toCharArray()); // otherwise password will be reset by CouchDB
            if (usersService.existsEmail(newUser.getEmail())) {
                // Someone else has in the meantime acquired this E-Mail address
                Logger.getAnonymousLogger().warning("Update Conflict: " + newUser.getEmail().getAddress() + " already exists!");
                return null;
            }
            return usersService.updateUser(auth, newUser);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user + "\n" + e.getMessage());
            return null;
        }
        catch (CredentialNotFoundException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

    public User getUser(String userName, String authHeader) throws BusinessException, TechnicalException, UnauthorizedException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.getUser(new Auth(creds), userName);
        }
        catch (DocumentNotFoundException e) {
            return null;
        }
        catch (CredentialNotFoundException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

    public User changePassword(User user, String rev, String authHeader) throws BusinessException, TechnicalException, UnauthorizedException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.changePassword(new Auth(creds), user);
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + user.getName() + "\n" + e.getMessage());
            return null;
        }
        catch (CredentialNotFoundException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

    public boolean deleteUser(String userName, String rev, String authHeader) throws BusinessException, TechnicalException, UnauthorizedException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            Auth auth = new Auth(creds);
            usersService.deleteUser(auth, userName, rev);
            return true;
        }
        catch (UpdateConflictException e) {
            Logger.getAnonymousLogger().warning("Update Conflict: " + userName + "\n" + e.getMessage());
            return false;
        }
        catch (CredentialNotFoundException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

}
