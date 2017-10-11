package de.bornemisza.couchdb;

import java.util.logging.Logger;

import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import de.bornemisza.couchdb.entity.CouchDbConnection;

public class HealthChecks {

    public HealthChecks() {
    }

    public boolean isCouchDbReady(CouchDbConnection conn) {
        if (conn == null) {
            Logger.getAnonymousLogger().warning("Null-CouchDbConnection");
            return false;
        }
        try {
            HttpClient httpClient = new StdHttpClient.Builder()
                        .url(conn.getBaseUrl())
                        .username(conn.getUserName())
                        .password(conn.getPassword())
                        .build();
            CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
            return dbInstance.checkIfDbExists(conn.getDatabaseName());
        }
        catch (DbAccessException e) {
            Logger.getAnonymousLogger().warning("CouchDB not ready: " + e.toString());
            return false;
        }
    }

}
