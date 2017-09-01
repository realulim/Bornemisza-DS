package de.bornemisza.rest.da;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.rest.HealthChecks;

import de.bornemisza.rest.Http;

public class HttpPool {
    
    public static String LIST_COUCHDB_HOSTQUEUE = "CouchDBHostQueue";
    public static String MAP_COUCHDB_UTILISATION = "CouchDBUtilisation";

    private final Map<String, Http> allConnections;
    private final List<String> couchDbHostQueue;
    private final Map<String, Integer> couchDbHostUtilisation;
    private final HealthChecks healthChecks;

    public HttpPool(Map<String, Http> connections, 
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

    public Http getConnection() {
        for (String hostname : couchDbHostQueue) {
            Http conn = allConnections.get(hostname);
            if (healthChecks.isCouchDbReady(conn)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
                Integer usageCount = this.couchDbHostUtilisation.get(hostname);
                couchDbHostUtilisation.put(hostname, ++usageCount);
                return conn;
            }
            else {
                Logger.getAnonymousLogger().info(hostname + " unreachable...");
            }
        }
        Logger.getAnonymousLogger().warning("No CouchDB Hosts available at all!");
        throw new RuntimeException("No CouchDB Backend ready!");
    }

}
