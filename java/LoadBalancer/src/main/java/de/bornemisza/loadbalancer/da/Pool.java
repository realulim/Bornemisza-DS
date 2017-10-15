package de.bornemisza.loadbalancer.da;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import de.bornemisza.loadbalancer.Config;

public abstract class Pool<T> {

    protected final Map<String, T> allConnections;
    private final HazelcastInstance hazelcast;

    private List<String> dbServerQueue = null;
    private Map<String, Integer> dbServerUtilisation = null;

    protected abstract String getServiceName();

    public Pool(Map<String, T> allConnections, HazelcastInstance hazelcast) {
        this.allConnections = allConnections;
        this.hazelcast = hazelcast;
        this.initCluster();
    }

    private void initCluster() {
        this.dbServerUtilisation = getDbServerUtilisation();
        this.dbServerQueue = sortHostnamesByUtilisation(allConnections.keySet());
    }

    public Set<String> getAllHostnames() {
        return this.allConnections.keySet();
    }

    protected List<String> getDbServerQueue() {
        return dbServerQueue;
    }

    List<String> sortHostnamesByUtilisation(Set<String> allHostnames) {
        return this.dbServerUtilisation.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue())
                .map(entry -> entry.getKey())
                .filter(key -> allHostnames.contains(key)) // in case we have stale Utilisation entries
                .collect(Collectors.toList());
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
            // no Hazelcast, so let's make it a plain map and try again next time
            Logger.getAnonymousLogger().warning("Hazelcast malfunctioning: " + e.toString());
            this.dbServerUtilisation = new HashMap<>(); // fallback, so clients can still work
        }
        populateDbServerUtilisation();
        return this.dbServerUtilisation;
    }

    protected void trackUtilisation(String hostname) {
        if (this.dbServerUtilisation == null) this.dbServerUtilisation = getDbServerUtilisation();
        this.dbServerUtilisation.computeIfPresent(hostname, (k, v) -> v+1);
    }

    private void populateDbServerUtilisation() {
        for (String newHostname : allConnections.keySet()) {
            if (! dbServerUtilisation.containsKey(newHostname)) {
                // reset utilisation for all hostname to start everyone on equal terms
                for (String oldHostname : dbServerUtilisation.keySet()) {
                    dbServerUtilisation.put(oldHostname, 0);
                }
                dbServerUtilisation.put(newHostname, 0);
            }
        }
    }

}
