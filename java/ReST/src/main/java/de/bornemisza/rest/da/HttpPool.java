package de.bornemisza.rest.da;

import java.util.Map;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;

public class HttpPool extends Pool<Http> {

    private final HealthChecks healthChecks;

    public HttpPool(@NotNull Map<String, Http> allConnections,
                    @NotNull HazelcastInstance hazelcast,
                    @NotNull HealthChecks healthChecks) {
        super(allConnections, hazelcast);
        this.healthChecks = healthChecks;
    }

    public Http getConnection() {
        for (String hostname : getDbServerQueue()) {
            Http conn = allConnections.get(hostname);
            if (healthChecks.isCouchDbReady(conn)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
                incrementRequestsFor(hostname);
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
