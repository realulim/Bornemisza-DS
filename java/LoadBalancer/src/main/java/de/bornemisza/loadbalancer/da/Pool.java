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

import de.bornemisza.loadbalancer.Config;

public abstract class Pool<T> {

    protected final Map<String, T> allConnections;
    private final HazelcastInstance hazelcast;

    private IList<String> dbServerQueue = null;
    protected List<String> dbServerQueueLocal = null;

    protected Map<String, Integer> dbServerUtilisation = null;

    public Pool(Map<String, T> allConnections, HazelcastInstance hazelcast) {
        this.allConnections = allConnections;
        this.hazelcast = hazelcast;
        this.initCluster();
    }

    private void initCluster() {
        this.dbServerQueue = createDbServerQueue();
        mirrorDbServerQueue();
        this.dbServerUtilisation = getDbServerUtilisation();
    }

    public Set<String> getAllHostnames() {
        return this.allConnections.keySet();
    }

    protected List<String> getDbServerQueue() {
        if (this.dbServerQueue == null) {
            // We are not connected to Hazelcast, so let's give it a try
            this.dbServerQueue = createDbServerQueue();
            mirrorDbServerQueue();
        }
        return this.dbServerQueueLocal;
    }

    private IList<String> createDbServerQueue() {
        try {
            // let's try to get a Hazelcast list now
            this.dbServerQueue = hazelcast.getList(Config.SERVERS);
        }
        catch (HazelcastException e) {
            // no Hazelcast, so try again later
            return null;
        }
        populateDbServerQueue();
        return this.dbServerQueue;
    }

    private void mirrorDbServerQueue() {
        List<String> mirroredQueue = new ArrayList<>();
        if (this.dbServerQueue != null) {
            mirroredQueue.addAll(this.dbServerQueue);
        }
Logger.getAnonymousLogger().info("Mirroring Queue: " + String.join("|", mirroredQueue));
        this.dbServerQueueLocal = mirroredQueue;
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

    protected void trackUtilisation(String hostname) {
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
