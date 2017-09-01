package de.bornemisza.rest.da;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;

public class HttpPool extends Pool {

    private final HealthChecks healthChecks;

    public HttpPool(@NotNull Map<String, Http> allConnections,
                    @NotNull HazelcastInstance hazelcast,
                    @NotNull HealthChecks healthChecks) {
        super(allConnections, hazelcast);
        this.healthChecks = healthChecks;
    }

    @Override
    protected Map<String, Http> getAllConnections() {
        return super.allConnections;
    }

    @Override
    protected List<String> getCouchDbHostQueue() {
        return super.couchDbHostQueue;
    }

    @Override
    protected Map<String, Integer> getCouchDbHostUtilisation() {
        return super.couchDbHostUtilisation;
    }

    public Http getConnection() {
        for (String hostname : getCouchDbHostQueue()) {
            Http conn = getAllConnections().get(hostname);
            if (healthChecks.isCouchDbReady(conn)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
                Integer usageCount = getCouchDbHostUtilisation().get(hostname);
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
