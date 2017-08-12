package de.bornemisza.users.da.couchdb;

import org.ektorp.CouchDbInstance;
import org.ektorp.impl.StdCouchDbConnector;

public class MyCouchDbConnector extends StdCouchDbConnector {

    private final String hostname;

    public MyCouchDbConnector(String hostname, String databaseName, CouchDbInstance dbInstance) {
        super(databaseName, dbInstance);
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

}
