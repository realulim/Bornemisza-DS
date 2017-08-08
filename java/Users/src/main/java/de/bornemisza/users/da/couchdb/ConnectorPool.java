package de.bornemisza.users.da.couchdb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import org.ektorp.CouchDbConnector;

import static de.bornemisza.users.JAXRSConfiguration.COUCHDB_HOSTQUEUE;
import static de.bornemisza.users.JAXRSConfiguration.COUCHDB_UTILISATION;

public class ConnectorPool {

    private final Map<String, CouchDbConnector> allConnectors;
    private final List<String> couchDbHostQueue;
    private final Map<String, Integer> couchDbHostUtilisation;
    private final HealthChecks healthChecks;

    public ConnectorPool(Map<String, CouchDbConnector> connectors, 
                         HazelcastInstance hazelcast,
                         HealthChecks healthChecks) {
        this.allConnectors = connectors;
        this.healthChecks = healthChecks;
        this.couchDbHostQueue = hazelcast.getList(COUCHDB_HOSTQUEUE);
        this.couchDbHostUtilisation = hazelcast.getMap(COUCHDB_UTILISATION);
        if (couchDbHostQueue.isEmpty()) {
            Set<String> hostnames = allConnectors.keySet();
            couchDbHostQueue.addAll(hostnames);
            for (String key : hostnames) {
                if (! couchDbHostUtilisation.containsKey(key)) {
                    couchDbHostUtilisation.put(key, 0);
                }
            }
        }
    }

    public CouchDbConnector getMember() {
        for (String hostname : couchDbHostQueue) {
            CouchDbConnector db = allConnectors.get(hostname);
            if (healthChecks.isHostAvailable(hostname, 443) && healthChecks.isCouchDbAvailable(db)) {
                Logger.getAnonymousLogger().info(hostname + " available, using it.");
                Integer usageCount = this.couchDbHostUtilisation.get(hostname);
                couchDbHostUtilisation.put(hostname, ++usageCount);
                return db;
            }
            else {
                Logger.getAnonymousLogger().info(hostname + " unreachable...");
            }
        }
        Logger.getAnonymousLogger().warning("No CouchDB Hosts available at all!");
        return null;
    }

}
