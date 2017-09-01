package de.bornemisza.loadbalancer.da;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hazelcast.core.HazelcastInstance;

public abstract class Pool<T> {

    public static String LIST_COUCHDB_HOSTQUEUE = "CouchDBHostQueue";
    public static String MAP_COUCHDB_UTILISATION = "CouchDBUtilisation";

    protected final Map<String, T> allConnections;
    protected final List<String> couchDbHostQueue;
    protected final Map<String, Integer> couchDbHostUtilisation;

    public Pool(Map<String, T> allConnections, HazelcastInstance hazelcast) {
        this.allConnections = allConnections;
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

    protected abstract Map<String, T> getAllConnections();
    protected abstract List<String> getCouchDbHostQueue();
    protected abstract Map<String, Integer> getCouchDbHostUtilisation();

}
