package de.bornemisza.users.da.couchdb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

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

    public boolean isCouchDbReady(HttpClient httpClient) {
        try {
            CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
            List<String> allDatabases = dbInstance.getAllDatabases();
            return allDatabases.size() > 0;
        }
        catch (DbAccessException e) {
            Logger.getAnonymousLogger().warning("CouchDB not ready: " + e.toString());
            return false;
        }
    }

}
