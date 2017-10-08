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

public abstract class Pool<T> {

    public static String LIST_COUCHDB_HOSTQUEUE = "CouchDBHostQueue";
    public static String MAP_COUCHDB_UTILISATION = "CouchDBUtilisation";

    protected final Map<String, T> allConnections;
    private final HazelcastInstance hazelcast;
    private List<String> couchDbHostQueue = null;
    private Map<String, Integer> couchDbHostUtilisation = null;

    public Pool(@NotNull Map<String, T> allConnections, @NotNull HazelcastInstance hazelcast) {
        this.allConnections = allConnections;
        this.hazelcast = hazelcast;
        this.initCluster();
    }

    private void initCluster() {
        this.couchDbHostQueue = getCouchDbHostQueue();
        this.couchDbHostUtilisation = getCouchDbHostUtilisation();
    }

    public List<String> getCouchDbHostQueue() {
        if (this.couchDbHostQueue != null && this.couchDbHostQueue instanceof IList) {
            // It's a Hazelcast list, so all is good
            return this.couchDbHostQueue;
        }
        // Not a Hazelcast list, so let's try to make it one
        try {
            this.couchDbHostQueue = hazelcast.getList(LIST_COUCHDB_HOSTQUEUE);
        }
        catch (HazelcastException e) {
            // still no Hazelcast, so let's make it a plain list and try again next time
            Logger.getAnonymousLogger().warning("Hazelcast malfunctioning: " + e.toString());
            if (this.couchDbHostQueue != null) return this.couchDbHostQueue;
            else this.couchDbHostQueue = new ArrayList<>(); // fallback, so clients can still work
        }
        fillCouchDbHostQueue();
        return this.couchDbHostQueue;
    }

    public Set<String> getAllHostnames() {
        return allConnections.keySet();
    }

    private void fillCouchDbHostQueue() {
        Set<String> hostnames = allConnections.keySet();
        if (couchDbHostQueue.isEmpty()) {
            couchDbHostQueue.addAll(hostnames);
        }
    }

    public Map<String, Integer> getCouchDbHostUtilisation() {
        if (this.couchDbHostUtilisation != null && this.couchDbHostUtilisation instanceof IMap) {
            // It's a Hazelcast map, so all is good
            return this.couchDbHostUtilisation;
        }
        // Not a Hazelcast map, so let's try to make it one
        try {
            this.couchDbHostUtilisation = hazelcast.getMap(MAP_COUCHDB_UTILISATION);
        }
        catch (HazelcastException e) {
            // still no Hazelcast, so let's make it a plain map and try again next time
            Logger.getAnonymousLogger().warning("Hazelcast malfunctioning: " + e.toString());
            if (this.couchDbHostUtilisation != null) return this.couchDbHostUtilisation;
            else this.couchDbHostUtilisation = new HashMap<>(); // fallback, so clients can still work
        }
        fillCouchDbHostUtilisation();
        return this.couchDbHostUtilisation;
    }

    public void incrementRequestsFor(String hostname) {
        if (this.couchDbHostUtilisation == null) this.couchDbHostUtilisation = getCouchDbHostUtilisation();
        this.couchDbHostUtilisation.compute(hostname, (k, v) -> v+1);
    }

    private void fillCouchDbHostUtilisation() {
        if (couchDbHostUtilisation.isEmpty()) {
            Set<String> hostnames = allConnections.keySet();
            for (String key : hostnames) {
                if (! couchDbHostUtilisation.containsKey(key)) {
                    couchDbHostUtilisation.put(key, 0);
                }
            }
        }
    }

}
