package de.bornemisza.users.da;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;

import org.ektorp.DbInfo;

import org.javalite.http.Delete;
import org.javalite.http.Get;
import org.javalite.http.HttpException;
import org.javalite.http.Put;

import de.bornemisza.couchdb.entity.MyCouchDbConnector;
import de.bornemisza.couchdb.entity.User;
import de.bornemisza.rest.Http;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.users.boundary.BusinessException;
import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.boundary.DocumentNotFoundException;
import de.bornemisza.users.boundary.TechnicalException;
import de.bornemisza.users.boundary.UnauthorizedException;
import de.bornemisza.users.boundary.UpdateConflictException;

public class UsersService {

    @Inject
    CouchAdminPool adminPool;

    @Inject
    HttpAdminPool usersPool;

    private static final String JSON_UTF8 = MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8").getType();

    public UsersService() {
    }

    @PostConstruct
    public void init() {
        MyCouchDbConnector adminConn = adminPool.getConnector();
        if (adminConn == null) throw new IllegalStateException("No Database reachable at all!");
        DbInfo dbInfo = adminConn.getDbInfo();
        String msg = "DB: " + dbInfo.getDbName() + ", Documents: " + dbInfo.getDocCount() + ", Disk Size: " + dbInfo.getDiskSize();
        Logger.getLogger(adminConn.getHostname()).info(msg);
    }

    public boolean existsUser(String userName) throws BusinessException, TechnicalException {
        User user = new User();
        user.setName(userName);
        Http http = usersPool.getConnection().getHttp();
        Get get = http.get(http.getBaseUrl() + http.urlEncode(user.getId()))
                .basic(usersPool.getUserName(), String.valueOf(usersPool.getPassword()))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        try {
            int responseCode = get.responseCode();
            switch (responseCode) {
                case 404:
                    return false;
                case 200:
                    return true;
                default:
                    throw new BusinessException(Type.UNEXPECTED, responseCode + ": " + get.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
    }

    public boolean existsEmail(InternetAddress email) {
        MyCouchDbConnector conn = adminPool.getConnector();
        UserRepository repo = new UserRepository(conn);
        return repo.findByEmail(email.getAddress()).size() > 0;
    }

    public User createUser(User user) throws BusinessException, TechnicalException, UpdateConflictException {
        Http http = usersPool.getConnection().getHttp();
        Put put = http.put(http.getBaseUrl() + http.urlEncode(user.getId()), http.toJson(user))
                .basic(usersPool.getUserName(), String.valueOf(usersPool.getPassword()))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, JSON_UTF8);
        try {
            int responseCode = put.responseCode();
            if (responseCode == 409) {
                throw new UpdateConflictException(put.responseMessage());
            }
            else if (responseCode < 201 || responseCode > 202) {
                throw new BusinessException(Type.UNEXPECTED, responseCode + ": " + put.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        User createdUser = readUser(user.getId(), new BasicAuthCredentials(usersPool.getUserName(), String.valueOf(usersPool.getPassword())));
        Logger.getLogger(http.getHostName()).info("Added user: " + createdUser);
        return createdUser;
    }

    public User updateUser(User user, BasicAuthCredentials creds) throws BusinessException, TechnicalException, UnauthorizedException, UpdateConflictException {
        Http http = usersPool.getConnection().getHttp();
        Put put = http.put(http.getBaseUrl() + http.urlEncode(user.getId()), http.toJson(user))
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
                throw new BusinessException(Type.UNEXPECTED, responseCode + ": " + put.responseMessage());
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
        Get get = http.get(http.getBaseUrl() + http.urlEncode(userId))
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
                throw new BusinessException(Type.UNEXPECTED, responseCode + ": " + get.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        return http.fromJson(get.text(), User.class);
    }

    public User changePassword(User user, BasicAuthCredentials creds) throws BusinessException, TechnicalException, UnauthorizedException, UpdateConflictException {
        Http http = usersPool.getConnection().getHttp();
        Put put = http.put(http.getBaseUrl() + http.urlEncode(user.getId()), http.toJson(user))
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
                throw new BusinessException(Type.UNEXPECTED, responseCode + ": " + put.responseMessage());
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
        Delete delete = http.delete(http.getBaseUrl() + http.urlEncode(user.getId()))
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
            else if (responseCode < 200 || responseCode > 202) {
                throw new BusinessException(Type.UNEXPECTED, responseCode + ": " + delete.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        Logger.getLogger(http.getHostName()).info("Removed user: " + userName);
    }

}
