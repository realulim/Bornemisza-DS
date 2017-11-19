package de.bornemisza.users.boundary;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.auth.login.CredentialNotFoundException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.couchdb.entity.User;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.boundary.BusinessException.Type;
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
            throw new BusinessException(Type.USER_ALREADY_EXISTS, user.getName() + " already exists!");
        }
        else if (usersService.existsEmail(user.getEmail())) {
            throw new BusinessException(Type.EMAIL_ALREADY_EXISTS, user.getEmail().getAddress() + " already exists!");
        }
        else {
            newUserAccountTopic.publish(user);
        }
    }

    public User confirmUser(String uuid) throws BusinessException, TechnicalException, UnauthorizedException {
        User user = newUserAccountMap_uuid.remove(uuid);
        if (user == null) {
            throw new BusinessException(Type.UUID_NOT_FOUND, uuid);
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
            throw new BusinessException(Type.UUID_NOT_FOUND, uuid);
        }
        changeEmailRequestMap_userId.delete(user.getId());
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
        catch (CredentialNotFoundException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

    public User getUser(String userName, String authHeader) throws BusinessException, TechnicalException, UnauthorizedException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(authHeader);
            return usersService.getUser(userName, creds);
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
            return usersService.changePassword(user, creds);
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
            usersService.deleteUser(userName, rev, creds);
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
