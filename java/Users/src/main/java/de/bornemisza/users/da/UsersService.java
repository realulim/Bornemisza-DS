package de.bornemisza.users.da;

import java.util.Arrays;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;

import org.ektorp.DbInfo;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.Options;
import org.ektorp.UpdateConflictException;

import de.bornemisza.couchdb.entity.MyCouchDbConnector;
import de.bornemisza.couchdb.entity.User;
import de.bornemisza.security.auth.BasicAuthCredentials;

public class UsersService {

    @Inject
    CouchUsersPool pool;

    @Inject
    CouchAdminPool adminPool;

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

    public boolean existsUser(String userName) {
        MyCouchDbConnector conn = adminPool.getConnector();
        UserRepository repo = new UserRepository(conn);
        userName = User.USERNAME_PREFIX + userName;
        try {
            return repo.get(userName) != null;
        }
        catch (DocumentNotFoundException e) {
            return false;
        }
    }

    public boolean existsEmail(InternetAddress email) {
        MyCouchDbConnector conn = adminPool.getConnector();
        UserRepository repo = new UserRepository(conn);
        return repo.findByEmail(email.getAddress()).size() > 0;
    }

    public User createUser(User user) throws UpdateConflictException {
        MyCouchDbConnector conn = adminPool.getConnector();
        UserRepository repo = new UserRepository(conn);
        repo.update(user); // CouchDB uses PUT not POST for user creation
        Arrays.fill(user.getPassword(), '*');
        Logger.getLogger(conn.getHostname()).info("Added user: " + user);
        return repo.get(user.getId());
    }

    public User updateUser(User user, BasicAuthCredentials creds) throws UpdateConflictException {
        MyCouchDbConnector conn = pool.getConnector(creds.getUserName(), creds.getPassword().toCharArray());
        UserRepository repo = new UserRepository(conn);
        repo.update(user);
        Logger.getLogger(conn.getHostname()).info("Updated user: " + user);
        return repo.get(user.getId());
    }

    public User getUser(String userName, BasicAuthCredentials creds) throws DocumentNotFoundException {
        return readUser(userName, creds);
    }

    private User readUser(String userName, BasicAuthCredentials creds) {
        return readUser(userName, null, creds);
    }

    private User readUser(String userName, Options options, BasicAuthCredentials creds) throws DocumentNotFoundException {
        MyCouchDbConnector conn = pool.getConnector(creds.getUserName(), creds.getPassword().toCharArray());
        UserRepository repo = new UserRepository(conn);
        userName = User.USERNAME_PREFIX + userName;
        User user = (options == null ? repo.get(userName) : repo.get(userName, options));
        Logger.getLogger(conn.getHostname()).fine("Read user: " + user);
        return user;
    }

    public User changePassword(User user, String ref, BasicAuthCredentials creds) throws UpdateConflictException {
        MyCouchDbConnector conn = pool.getConnector(creds.getUserName(), creds.getPassword().toCharArray());
        UserRepository repo = new UserRepository(conn);
        repo.update(user);
        Logger.getLogger(conn.getHostname()).info("Changed password for user: " + user);

        // change credentials to reflect new password
        creds.changePassword(String.valueOf(user.getPassword()));
        conn = pool.getConnector(creds.getUserName(), creds.getPassword().toCharArray());
        repo = new UserRepository(conn);
        return repo.get(user.getId());
    }

    public void deleteUser(String userName, String rev, BasicAuthCredentials creds) throws UpdateConflictException {
        MyCouchDbConnector conn = pool.getConnector(creds.getUserName(), creds.getPassword().toCharArray());
        UserRepository repo = new UserRepository(conn);
        User user = new User();
        user.setName(userName);
        user.setRevision(rev);
        repo.remove(user);
        Logger.getLogger(conn.getHostname()).info("Removed user: " + user);
    }

}
