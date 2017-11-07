package de.bornemisza.maintenance.task;

import javax.ejb.Stateless;

import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import de.bornemisza.couchdb.entity.CouchDbConnection;

@Stateless
public class HealthChecks {
    
    public boolean isCouchDbReady(CouchDbConnection conn) {
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
            return false;
        }
    }

}
