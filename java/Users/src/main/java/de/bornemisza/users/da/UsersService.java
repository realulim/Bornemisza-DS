package de.bornemisza.users.da;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.MediaType;

import org.javalite.common.Util;
import org.javalite.http.Delete;
import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.javalite.http.Put;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.Json;
import de.bornemisza.rest.entity.Database;
import de.bornemisza.rest.entity.KeyValueViewResult;
import de.bornemisza.rest.entity.KeyValueViewResult.Row;
import de.bornemisza.rest.entity.User;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.DocumentNotFoundException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.exception.UpdateConflictException;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.users.boundary.UsersType;

public class UsersService {

    @Inject
    CouchUsersPoolAsAdmin usersPoolAsAdmin;

    @Inject
    CouchUsersPool usersPool;

    private static final String JSON_UTF8 = MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8").getType();

    public UsersService() {
    }

    // Constructor for Unit Tests
    public UsersService(CouchUsersPoolAsAdmin pool1, CouchUsersPool pool2) {
        this.usersPoolAsAdmin = pool1;
        this.usersPool = pool2;
    }

    @PostConstruct
    public void init() {
        Http http = usersPool.getConnection().getHttp();
        Get get = http.get(http.getBaseUrl())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        try {
            int responseCode = get.responseCode();
            if (responseCode != 200) {
                throw new BusinessException(UsersType.UNEXPECTED, responseCode + ": " + get.responseMessage());
            }
            else {
                Database db = Json.fromJson(get.text(), Database.class);
                String msg = "DB: " + db.getDbName() + ", Documents: " + db.getDocCount() + ", Disk Size: " + db.getDiskSize();
                Logger.getLogger(http.getHostName()).info(msg);
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
    }

    public boolean existsUser(String userName) throws BusinessException, TechnicalException {
        User user = new User();
        user.setName(userName);
        Http http = usersPoolAsAdmin.getConnection().getHttp();
        Get get = http.get(http.getBaseUrl() + Util.urlEncode(user.getId()))
                .basic(usersPoolAsAdmin.getUserName(), String.valueOf(usersPoolAsAdmin.getPassword()))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        try {
            int responseCode = get.responseCode();
            switch (responseCode) {
                case 404:
                    return false;
                case 200:
                    return true;
                default:
                    throw new BusinessException(UsersType.UNEXPECTED, responseCode + ": " + get.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
    }

    public boolean existsEmail(InternetAddress email) throws BusinessException, TechnicalException {
        Http http = usersPoolAsAdmin.getConnection().getHttp();
        Get get = http.get(http.getBaseUrl() + "_design/User/_view/by_email")
                .basic(usersPoolAsAdmin.getUserName(), String.valueOf(usersPoolAsAdmin.getPassword()))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        try {
            int responseCode = get.responseCode();
            if (responseCode != 200) {
                throw new BusinessException(UsersType.UNEXPECTED, responseCode + ": " + get.responseMessage());
            }
            else {
                String json = get.text();
                KeyValueViewResult viewResult = Json.fromJson(json, KeyValueViewResult.class);
                String emailStr = email.getAddress();
                for (Row row : viewResult.getRows()) {
                    if (emailStr.equals(row.getKey())) return true;
                }
                return false;
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
    }

    public User createUser(User user) throws BusinessException, TechnicalException, UpdateConflictException {
        Http http = usersPoolAsAdmin.getConnection().getHttp();
        Put put = http.put(http.getBaseUrl() + Util.urlEncode(user.getId()), Json.toJson(user))
                .basic(usersPoolAsAdmin.getUserName(), String.valueOf(usersPoolAsAdmin.getPassword()))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, JSON_UTF8);
        try {
            int responseCode = put.responseCode();
            if (responseCode == 409) {
                throw new UpdateConflictException(put.responseMessage());
            }
            else if (responseCode < 201 || responseCode > 202) {
                throw new BusinessException(UsersType.UNEXPECTED, responseCode + ": " + put.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        User createdUser = readUser(user.getId(), new BasicAuthCredentials(usersPoolAsAdmin.getUserName(), String.valueOf(usersPoolAsAdmin.getPassword())));
        Logger.getLogger(http.getHostName()).info("Added user: " + createdUser);
        return createdUser;
    }

    public User updateUser(User user, BasicAuthCredentials creds) throws BusinessException, TechnicalException, UnauthorizedException, UpdateConflictException {
        Http http = usersPool.getConnection().getHttp();
        Put put = http.put(http.getBaseUrl() + Util.urlEncode(user.getId()), Json.toJson(user))
                .basic(creds.getUserName(), creds.getPassword())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, JSON_UTF8);
        try {
            int responseCode = put.responseCode();
            if (responseCode == 401) {
                throw new UnauthorizedException(put.responseMessage());
            }
            else if (responseCode == 409) {
                throw new UpdateConflictException(put.responseMessage());
            }
            else if (responseCode < 201 || responseCode > 202) {
                throw new BusinessException(UsersType.UNEXPECTED, responseCode + ": " + put.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        User updatedUser = readUser(user.getId(), creds);
        Logger.getLogger(http.getHostName()).info("Updated user: " + updatedUser);
        return updatedUser;
    }

    public User getUser(String userName, BasicAuthCredentials creds) throws BusinessException, DocumentNotFoundException, UnauthorizedException {
        User user = new User();
        user.setName(userName);
        return readUser(user.getId(), creds);
    }

    private User readUser(String userId, BasicAuthCredentials creds) throws BusinessException, DocumentNotFoundException, TechnicalException, UnauthorizedException {
        Http http = usersPool.getConnection().getHttp();
        Get get = http.get(http.getBaseUrl() + Util.urlEncode(userId))
                .basic(creds.getUserName(), creds.getPassword())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        try {
            int responseCode = get.responseCode();
            if (responseCode == 401) {
                throw new UnauthorizedException(get.responseMessage());
            }
            else if (responseCode == 404) {
                throw new DocumentNotFoundException(get.responseMessage());
            }
            else if (responseCode != 200) {
                throw new BusinessException(UsersType.UNEXPECTED, responseCode + ": " + get.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        return Json.fromJson(get.text(), User.class);
    }

    public User changePassword(User user, BasicAuthCredentials creds) throws BusinessException, TechnicalException, UnauthorizedException, UpdateConflictException {
        Http http = usersPool.getConnection().getHttp();
        Put put = http.put(http.getBaseUrl() + Util.urlEncode(user.getId()), Json.toJson(user))
                .basic(creds.getUserName(), creds.getPassword())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, JSON_UTF8);
        try {
            int responseCode = put.responseCode();
            if (responseCode == 401) {
                throw new UnauthorizedException(put.responseMessage());
            }
            else if (responseCode == 409) {
                throw new UpdateConflictException(put.responseMessage());
            }
            else if (responseCode < 201 || responseCode > 202) {
                throw new BusinessException(UsersType.UNEXPECTED, responseCode + ": " + put.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        Logger.getLogger(http.getHostName()).info("Changed password for user: " + user);

        // change credentials to reflect new password
        creds.changePassword(String.valueOf(user.getPassword()));
        return readUser(user.getId(), creds);
    }

    public void deleteUser(String userName, String rev, BasicAuthCredentials creds) throws BusinessException, TechnicalException, UnauthorizedException, UpdateConflictException {
        User user = new User();
        user.setName(userName);
        Http http = usersPool.getConnection().getHttp();
        Delete delete = http.delete(http.getBaseUrl() + Util.urlEncode(user.getId()))
                .basic(creds.getUserName(), creds.getPassword())
                .header(HttpHeaders.IF_MATCH, rev);
        try {
            int responseCode = delete.responseCode();
            if (responseCode == 401) {
                throw new UnauthorizedException(delete.responseMessage());
            }
            else if (responseCode == 409) {
                throw new UpdateConflictException(delete.responseMessage());
            }
            else if (responseCode < 200 || responseCode > 202 || responseCode == 201) {
                throw new BusinessException(UsersType.UNEXPECTED, responseCode + ": " + delete.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        Logger.getLogger(http.getHostName()).info("Removed user: " + userName);
    }

}
