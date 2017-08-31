package de.bornemisza.couchdb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    public boolean isHostAvailable(final String hostname, final int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostname, port), 1000);
            return true;
        } 
        catch (IOException ex) {
            return false;
        }
    }

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
            Logger.getAnonymousLogger().warning("CouchDB not ready: " + e.toString());
            return false;
        }
    }

}
