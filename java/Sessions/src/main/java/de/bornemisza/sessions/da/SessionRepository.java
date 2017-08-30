package de.bornemisza.sessions.da;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import de.bornemisza.rest.entity.Session;

public class SessionRepository extends CouchDbRepositorySupport<Session> {

    public SessionRepository(CouchDbConnector db) {
        super(Session.class, db);
    }

}
