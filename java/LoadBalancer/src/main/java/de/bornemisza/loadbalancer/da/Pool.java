package de.bornemisza.loadbalancer.da;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;

public abstract class Pool<T> {

    public static String LIST_COUCHDB_HOSTQUEUE = "CouchDBHostQueue";
    public static String MAP_COUCHDB_UTILISATION = "CouchDBUtilisation";

    protected final Map<String, T> allConnections;
    private List<String> couchDbHostQueue;
    private Map<String, Integer> couchDbHostUtilisation;
    private final HazelcastInstance hazelcast;

    public Pool(Map<String, T> allConnections, HazelcastInstance hazelcast) {
        this.allConnections = allConnections;
        this.hazelcast = hazelcast;
        createCouchDbHostQueue();
        initCouchDbHostQueue();
        createCouchDbHostUtilisation();
        initCouchDbHostUtilisation();
    }

    private void createCouchDbHostQueue() {
        this.couchDbHostQueue = getCouchDbHostQueue();
    }

    private void initCouchDbHostQueue() {
        if (couchDbHostQueue.isEmpty()) {
            Set<String> hostnames = allConnections.keySet();
            couchDbHostQueue.addAll(hostnames);
        }
    }

    private void createCouchDbHostUtilisation() {
        this.couchDbHostUtilisation = getCouchDbHostUtilisation();
    }

    private void initCouchDbHostUtilisation() {
        if (couchDbHostUtilisation.isEmpty()) {
            Set<String> hostnames = allConnections.keySet();
            for (String key : hostnames) {
                if (! couchDbHostUtilisation.containsKey(key)) {
                    couchDbHostUtilisation.put(key, 0);
                }
            }
        }
    }

    public List<String> getCouchDbHostQueue() {
        if (this.couchDbHostQueue instanceof IList) {
            // It's a Hazelcast list, so all is good
            return this.couchDbHostQueue;
        }
        // Not a Hazelcast list, so let's try to make it one
        try {
            this.couchDbHostQueue = hazelcast.getList(LIST_COUCHDB_HOSTQUEUE);
            initCouchDbHostQueue();
        }
        catch (HazelcastException e) {
            // still no Hazelcast, so let's make it a plain list and try again next time
            Logger.getAnonymousLogger().warning("Hazelcast malfunctioning: " + e.toString());
            this.couchDbHostQueue = new ArrayList<>(); // fallback, so clients can still work
        }
        return this.couchDbHostQueue;
    }

    public Map<String, Integer> getCouchDbHostUtilisation() {
        if (this.couchDbHostUtilisation instanceof IMap) {
            // It's a Hazelcast map, so all is good
            return this.couchDbHostUtilisation;
        }
        // Not a Hazelcast map, so let's try to make it one
        try {
            this.couchDbHostUtilisation = hazelcast.getMap(MAP_COUCHDB_UTILISATION);
            initCouchDbHostUtilisation();
        }
        catch (HazelcastException e) {
            // still no Hazelcast, so let's make it a plain map and try again next time
            Logger.getAnonymousLogger().warning("Hazelcast malfunctioning: " + e.toString());
            this.couchDbHostUtilisation = new HashMap<>(); // fallback, so clients can still work
        }
        return this.couchDbHostUtilisation;
    }

}
