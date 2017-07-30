package de.bornemisza.users.da;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import de.bornemisza.users.entity.User;

public class UsersRepository extends CouchDbRepositorySupport<User> {

    public UsersRepository(CouchDbConnector db) {
        super(User.class, db);
    }

}
