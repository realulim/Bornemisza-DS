package de.bornemisza.rest.da;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;

public class HttpPool extends Pool<Http> {

    private final HealthChecks healthChecks;

    public HttpPool(Map<String, Http> allConnections,
                    HazelcastInstance hazelcast,
                    HealthChecks healthChecks) {
        super(allConnections, hazelcast);
        this.healthChecks = healthChecks;
    }

    public Http getConnection() {
        List<String> dbServerQueue = getDbServerQueue();
        for (String hostname : dbServerQueue) {
            Http conn = allConnections.get(hostname);
            if (healthChecks.isCouchDbReady(conn)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
long start = System.currentTimeMillis();
                trackUtilisation(hostname);
long duration = System.currentTimeMillis() - start;
Logger.getAnonymousLogger().info("Track Utilisation: " + duration);
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
