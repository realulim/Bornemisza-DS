package de.bornemisza.users.da;

import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import de.bornemisza.rest.entity.User;

public class UserRepository extends CouchDbRepositorySupport<User> {

    public UserRepository(CouchDbConnector db) {
        super(User.class, db);
    }

    public List<User> findByEmail(String email) {
        return queryView("by_email", email);
    }

}
