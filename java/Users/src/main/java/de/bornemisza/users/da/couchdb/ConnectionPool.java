package de.bornemisza.users.da.couchdb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.users.boundary.BasicAuthCredentials;
import de.bornemisza.users.da.CouchDbConnection;
import static de.bornemisza.users.JAXRSConfiguration.LIST_COUCHDB_HOSTQUEUE;
import static de.bornemisza.users.JAXRSConfiguration.MAP_COUCHDB_UTILISATION;

public class ConnectionPool {

    private final Map<String, CouchDbConnection> allConnections;
    private final List<String> couchDbHostQueue;
    private final Map<String, Integer> couchDbHostUtilisation;
    private final HealthChecks healthChecks;

    public ConnectionPool(Map<String, CouchDbConnection> connections, 
                         HazelcastInstance hazelcast,
                         HealthChecks healthChecks) {
        this.allConnections = connections;
        this.healthChecks = healthChecks;
        this.couchDbHostQueue = hazelcast.getList(LIST_COUCHDB_HOSTQUEUE);
        this.couchDbHostUtilisation = hazelcast.getMap(MAP_COUCHDB_UTILISATION);
        if (couchDbHostQueue.isEmpty()) {
            Set<String> hostnames = allConnections.keySet();
            couchDbHostQueue.addAll(hostnames);
            for (String key : hostnames) {
                if (! couchDbHostUtilisation.containsKey(key)) {
                    couchDbHostUtilisation.put(key, 0);
                }
            }
        }
    }

    public MyCouchDbConnector getConnection() {
        return getConnection(null);
    }

    public MyCouchDbConnector getConnection(BasicAuthCredentials creds) {
        for (String hostname : couchDbHostQueue) {
            CouchDbConnection conn = allConnections.get(hostname);
            HttpClient httpClient = createHttpClient(conn, creds);
            if (healthChecks.isCouchDbReady(httpClient)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
                Integer usageCount = this.couchDbHostUtilisation.get(hostname);
                couchDbHostUtilisation.put(hostname, ++usageCount);
                CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
                return new MyCouchDbConnector(hostname, conn.getDatabaseName(), dbInstance);
            }
            else {
                Logger.getAnonymousLogger().info(hostname + " unreachable...");
            }
        }
        Logger.getAnonymousLogger().warning("No CouchDB Hosts available at all!");
        throw new DbAccessException("No CouchDB Backend ready!");
    }

    private HttpClient createHttpClient(CouchDbConnection conn, BasicAuthCredentials creds) {
        if (creds == null) return new StdHttpClient.Builder()
                    .url(conn.getUrl())
                    .username(conn.getUserName())
                    .password(conn.getPassword())
                    .build();
        else return new StdHttpClient.Builder()
                    .url(conn.getUrl())
                    .username(creds.getUserName())
                    .password(creds.getPassword())
                    .build();
    }

}
