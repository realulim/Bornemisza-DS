package de.bornemisza.users.da;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.ektorp.CouchDbConnector;
import org.ektorp.DbInfo;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.Options;
import org.ektorp.UpdateConflictException;

import de.bornemisza.users.da.couchdb.ConnectionPool;
import de.bornemisza.users.entity.User;

public class UsersService {

    @Resource(name="couchdb/Users")
    ConnectionPool pool;

    private UsersRepository repo;

    public UsersService() {
    }

    @PostConstruct
    public void init() {
        CouchDbConnector conn = pool.getConnection();
        if (conn == null) throw new IllegalStateException("No Database reachable at all!");
        DbInfo dbInfo = conn.getDbInfo();
        String msg = "DB: " + dbInfo.getDbName() + ", Documents: " + dbInfo.getDocCount() + ", Disk Size: " + dbInfo.getDiskSize();
        Logger.getAnonymousLogger().info(msg);
        repo = new UsersRepository(conn);
    }

    public User createUser(User user) throws UpdateConflictException {
        repo.add(user);
        Logger.getAnonymousLogger().info("Added user: " + user);
        return user;
    }

    public User updateUser(User user) throws UpdateConflictException {
        repo.update(user);
        Logger.getAnonymousLogger().info("Updated user: " + user);
        return user;
    }

    public User getUser(String userName) throws DocumentNotFoundException {
        return readUser(userName);
    }

    private User readUser(String userName) {
        return readUser(userName, null);
    }

    private User readUser(String userName, Options options) throws DocumentNotFoundException {
        userName = User.USERNAME_PREFIX + userName;
        User user = (options == null ? repo.get(userName) : repo.get(userName, options));
        Logger.getAnonymousLogger().info("Read user: " + user);
        return user;
    }

    public void deleteUser(String userName, String rev) throws UpdateConflictException {
        User user = new User();
        user.setName(userName);
        user.setRevision(rev);
        repo.remove(user);
        Logger.getAnonymousLogger().info("Removed user: " + user);
    }

}
