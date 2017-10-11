package de.bornemisza.rest.da;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;
import java.util.Arrays;

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
Logger.getAnonymousLogger().info("Queue 2: " + Arrays.toString(dbServerQueue.toArray()));
        for (String hostname : dbServerQueue) {
            Http conn = allConnections.get(hostname);
            if (healthChecks.isCouchDbReady(conn)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
                trackUtilisation(hostname);
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
