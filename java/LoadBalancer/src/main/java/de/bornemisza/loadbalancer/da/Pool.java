package de.bornemisza.loadbalancer.da;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;

import de.bornemisza.loadbalancer.Config;

public abstract class Pool<T> {

    protected final Map<String, T> allConnections;
    private final HazelcastInstance hazelcast;
    private List<String> dbServerQueue = null;
    private Map<String, Integer> dbServerUtilisation = null;

    public Pool(@NotNull Map<String, T> allConnections, @NotNull HazelcastInstance hazelcast) {
        this.allConnections = allConnections;
        this.hazelcast = hazelcast;
        this.initCluster();
    }

    private void initCluster() {
        this.dbServerQueue = getDbServerQueue();
        this.dbServerUtilisation = getDbServerUtilisation();
    }

    protected List<String> getDbServerQueue() {
        if (this.dbServerQueue != null && this.dbServerQueue instanceof IList) {
            // It's a Hazelcast list, so all is good
            return this.dbServerQueue;
        }
        // Not a Hazelcast list, so let's try to make it one
        try {
            this.dbServerQueue = hazelcast.getList(Config.SERVERS);
        }
        catch (HazelcastException e) {
            // still no Hazelcast, so let's make it a plain list and try again next time
            Logger.getAnonymousLogger().warning("Hazelcast malfunctioning: " + e.toString());
            if (this.dbServerQueue != null) return this.dbServerQueue;
            else this.dbServerQueue = new ArrayList<>(); // fallback, so clients can still work
        }
        populateDbServerQueue();
        return this.dbServerQueue;
    }

    private void populateDbServerQueue() {
        Set<String> hostnames = allConnections.keySet();
        if (dbServerQueue.isEmpty()) {
            dbServerQueue.addAll(hostnames);
        }
    }

    protected Map<String, Integer> getDbServerUtilisation() {
        if (this.dbServerUtilisation != null && this.dbServerUtilisation instanceof IMap) {
            // It's a Hazelcast map, so all is good
            return this.dbServerUtilisation;
        }
        // Not a Hazelcast map, so let's try to make it one
        try {
            this.dbServerUtilisation = hazelcast.getMap(Config.UTILISATION);
        }
        catch (HazelcastException e) {
            // still no Hazelcast, so let's make it a plain map and try again next time
            Logger.getAnonymousLogger().warning("Hazelcast malfunctioning: " + e.toString());
            if (this.dbServerUtilisation != null) return this.dbServerUtilisation;
            else this.dbServerUtilisation = new HashMap<>(); // fallback, so clients can still work
        }
        populateDbServerUtilisation();
        return this.dbServerUtilisation;
    }

    protected void incrementRequestsFor(String hostname) {
        if (this.dbServerUtilisation == null) this.dbServerUtilisation = getDbServerUtilisation();
        this.dbServerUtilisation.compute(hostname, (k, v) -> v+1);
    }

    private void populateDbServerUtilisation() {
        Set<String> hostnames = allConnections.keySet();
        if (dbServerUtilisation.isEmpty()) {
            for (String key : hostnames) {
                if (! dbServerUtilisation.containsKey(key)) {
                    dbServerUtilisation.put(key, 0);
                }
            }
        }
    }

}
