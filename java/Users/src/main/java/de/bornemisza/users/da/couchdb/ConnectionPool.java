package de.bornemisza.users.da.couchdb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.users.da.CouchDbConnection;
import static de.bornemisza.users.JAXRSConfiguration.COUCHDB_HOSTQUEUE;
import static de.bornemisza.users.JAXRSConfiguration.COUCHDB_UTILISATION;

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
        this.couchDbHostQueue = hazelcast.getList(COUCHDB_HOSTQUEUE);
        this.couchDbHostUtilisation = hazelcast.getMap(COUCHDB_UTILISATION);
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

    public CouchDbConnector getConnection() {
        for (String hostname : couchDbHostQueue) {
            CouchDbConnection conn = allConnections.get(hostname);
            HttpClient httpClient = createHttpClient(conn);
            if (healthChecks.isCouchDbReady(httpClient)) {
                Logger.getAnonymousLogger().info(hostname + " available, using it.");
                Integer usageCount = this.couchDbHostUtilisation.get(hostname);
                couchDbHostUtilisation.put(hostname, ++usageCount);
                CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
                return new StdCouchDbConnector(conn.getDatabaseName(), dbInstance);
            }
            else {
                Logger.getAnonymousLogger().info(hostname + " unreachable...");
            }
        }
        Logger.getAnonymousLogger().warning("No CouchDB Hosts available at all!");
        return null;
    }

    private HttpClient createHttpClient(CouchDbConnection conn) {
        return new StdHttpClient.Builder()
                .url(conn.getUrl())
                .username(conn.getUserName())
                .password(conn.getPassword())
                .build();
    }

}
