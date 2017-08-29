package de.bornemisza.users.da;

import java.util.Arrays;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.ektorp.DbInfo;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.Options;
import org.ektorp.UpdateConflictException;

import de.bornemisza.users.boundary.BasicAuthCredentials;
import de.bornemisza.users.da.couchdb.ConnectionPool;
import de.bornemisza.users.da.couchdb.MyCouchDbConnector;
import de.bornemisza.users.entity.User;

public class UsersService {

    @Resource(name="couchdb/Users")
    ConnectionPool pool;

    @Resource(name="couchdb/admin")
    ConnectionPool adminPool;

    private UserRepository adminRepo;

    public UsersService() {
    }

    @PostConstruct
    public void init() {
        MyCouchDbConnector adminConn = adminPool.getConnection();
        if (adminConn == null) throw new IllegalStateException("No Database reachable at all!");
        DbInfo dbInfo = adminConn.getDbInfo();
        String msg = "DB: " + dbInfo.getDbName() + ", Documents: " + dbInfo.getDocCount() + ", Disk Size: " + dbInfo.getDiskSize();
        Logger.getLogger(adminConn.getHostname()).info(msg);
        adminRepo = new UserRepository(adminConn);
    }

    public User createUser(User user) {
        MyCouchDbConnector conn = adminPool.getConnection();
        UserRepository repo = new UserRepository(conn);
        repo.update(user); // CouchDB uses PUT not POST for user creation
        Logger.getLogger(conn.getHostname()).info("Added user: " + user);
        Arrays.fill(user.getPassword(), '*');
        return user;
    }

    public User updateUser(User user, BasicAuthCredentials creds) {
        MyCouchDbConnector conn = pool.getConnection(creds);
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
        MyCouchDbConnector conn = pool.getConnection(creds);
        UserRepository repo = new UserRepository(conn);
        userName = User.USERNAME_PREFIX + userName;
        User user = (options == null ? repo.get(userName) : repo.get(userName, options));
        Logger.getLogger(conn.getHostname()).info("Read user: " + user);
        return user;
    }

    public User changePassword(User user, String ref, BasicAuthCredentials creds) throws UpdateConflictException {
        MyCouchDbConnector conn = pool.getConnection(creds);
        UserRepository repo = new UserRepository(conn);
        repo.update(user);
        Logger.getLogger(conn.getHostname()).info("Changed password for user: " + user);

        // change credentials to reflect new password
        creds.changePassword(String.valueOf(user.getPassword()));
        conn = pool.getConnection(creds);
        repo = new UserRepository(conn);
        return repo.get(user.getId());
    }

    public void deleteUser(String userName, String rev, BasicAuthCredentials creds) throws UpdateConflictException {
        MyCouchDbConnector conn = pool.getConnection(creds);
        UserRepository repo = new UserRepository(conn);
        User user = new User();
        user.setName(userName);
        user.setRevision(rev);
        repo.remove(user);
        Logger.getLogger(conn.getHostname()).info("Removed user: " + user);
    }

}
