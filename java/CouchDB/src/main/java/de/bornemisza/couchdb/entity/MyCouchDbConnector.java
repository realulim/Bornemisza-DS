package de.bornemisza.couchdb.entity;

import org.ektorp.CouchDbInstance;
import org.ektorp.impl.StdCouchDbConnector;

public class MyCouchDbConnector extends StdCouchDbConnector {

    private final String hostname;
    private final CouchDbConnection conn;

    public MyCouchDbConnector(String hostname, CouchDbConnection conn, CouchDbInstance dbInstance) {
        super(conn.getDatabaseName(), dbInstance);
        this.hostname = hostname;
        this.conn = conn;
    }

    public String getHostname() {
        return this.hostname;
    }

    public CouchDbConnection getCouchDbConnection() {
        return this.conn;
    }

}
