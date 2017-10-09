package de.bornemisza.couchdb.da;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;

import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.couchdb.HealthChecks;
import de.bornemisza.couchdb.entity.CouchDbConnection;
import de.bornemisza.couchdb.entity.MyCouchDbConnector;
import de.bornemisza.loadbalancer.da.Pool;

public class ConnectionPool extends Pool<CouchDbConnection> {

    private final HealthChecks healthChecks;

    public ConnectionPool(@NotNull Map<String, CouchDbConnection> connections, 
                         @NotNull HazelcastInstance hazelcast,
                         @NotNull HealthChecks healthChecks) {
        super(connections, hazelcast);
        this.healthChecks = healthChecks;
    }

    public MyCouchDbConnector getConnector() {
        return getConnector(null, null);
    }

    public MyCouchDbConnector getConnector(String userName, char[] password) {
        for (String hostname : getDbServerQueue()) {
            CouchDbConnection conn = allConnections.get(hostname);
            if (healthChecks.isCouchDbReady(conn)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
                incrementRequestsFor(hostname);
                HttpClient httpClient = createHttpClient(conn, userName, password);
                if (password != null) Arrays.fill(password, '*');
                CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
                return new MyCouchDbConnector(hostname, conn, dbInstance);
            }
            else {
                Logger.getAnonymousLogger().info(hostname + " unreachable...");
            }
        }
        Logger.getAnonymousLogger().warning("No CouchDB Hosts available at all!");
        throw new DbAccessException("No CouchDB Backend ready!");
    }

    private HttpClient createHttpClient(CouchDbConnection conn, String userName, char[] password) {
        if (userName == null) return new StdHttpClient.Builder()
                    .url(conn.getBaseUrl())
                    .username(conn.getUserName())
                    .password(conn.getPassword())
                    .build();
        else return new StdHttpClient.Builder()
                    .url(conn.getBaseUrl())
                    .username(userName)
                    .password(String.valueOf(password))
                    .build();
    }

}
