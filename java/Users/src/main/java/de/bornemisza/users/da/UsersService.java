package de.bornemisza.users.da;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.ektorp.CouchDbConnector;
import org.ektorp.DbInfo;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.Options;
import org.ektorp.UpdateConflictException;

import de.bornemisza.users.boundary.BasicAuthCredentials;
import de.bornemisza.users.da.couchdb.ConnectionPool;
import de.bornemisza.users.entity.User;

public class UsersService {

    @Resource(name="couchdb/Users")
    ConnectionPool pool;

    @Resource(name="admin@couchdb/Users")
    ConnectionPool adminPool;

    private UsersRepository adminRepo;

    public UsersService() {
    }

    @PostConstruct
    public void init() {
        CouchDbConnector adminConn = adminPool.getConnection();
        if (adminConn == null) throw new IllegalStateException("No Database reachable at all!");
        DbInfo dbInfo = adminConn.getDbInfo();
        String msg = "DB: " + dbInfo.getDbName() + ", Documents: " + dbInfo.getDocCount() + ", Disk Size: " + dbInfo.getDiskSize();
        Logger.getAnonymousLogger().info(msg);
        adminRepo = new UsersRepository(adminConn);
    }

    public User createUser(User user, BasicAuthCredentials creds) {
        UsersRepository repo = new UsersRepository(pool.getConnection(creds));
        repo.add(user);
        Logger.getAnonymousLogger().info("Added user: " + user);
        return user;
    }

    public User updateUser(User user, BasicAuthCredentials creds) throws UpdateConflictException {
        UsersRepository repo = new UsersRepository(pool.getConnection(creds));
        repo.update(user);
        Logger.getAnonymousLogger().info("Updated user: " + user);
        return user;
    }

    public User getUser(String userName, BasicAuthCredentials creds) throws DocumentNotFoundException {
        return readUser(userName, creds);
    }

    private User readUser(String userName, BasicAuthCredentials creds) {
        return readUser(userName, null, creds);
    }

    private User readUser(String userName, Options options, BasicAuthCredentials creds) throws DocumentNotFoundException {
        userName = User.USERNAME_PREFIX + userName;
        UsersRepository repo = new UsersRepository(pool.getConnection(creds));
        User user = (options == null ? repo.get(userName) : repo.get(userName, options));
        Logger.getAnonymousLogger().info("Read user: " + user);
        return user;
    }

    public void deleteUser(String userName, String rev, BasicAuthCredentials creds) throws UpdateConflictException {
        User user = new User();
        user.setName(userName);
        user.setRevision(rev);
        UsersRepository repo = new UsersRepository(pool.getConnection(creds));
        repo.remove(user);
        Logger.getAnonymousLogger().info("Removed user: " + user);
    }

}
