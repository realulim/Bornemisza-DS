package de.bornemisza.users.da;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import de.bornemisza.users.entity.User;

public class UserRepository extends CouchDbRepositorySupport<User> {

    public UserRepository(CouchDbConnector db) {
        super(User.class, db);
    }

}
