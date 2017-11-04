package de.bornemisza.loadbalancer.da;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import de.bornemisza.loadbalancer.Config;

public abstract class Pool<T> {

    @Inject
    protected HazelcastInstance hazelcast;

    protected DnsProvider dnsProvider;
    protected Map<String, T> allConnections;

    private Map<String, Integer> dbServerUtilisation = null;

    protected abstract String getServiceName();
    protected abstract Map<String, T> createConnections();

    public Pool() { }

    // Constructor for Unit Tests
    public Pool(HazelcastInstance hazelcast, DnsProvider dnsProvider) {
        this.hazelcast = hazelcast;
        this.dnsProvider = dnsProvider;
        this.initCluster();
    }

    @PostConstruct
    protected void init() {
        this.dnsProvider = new DnsProvider(hazelcast);
        this.initCluster();
    }

    private void initCluster() {
        this.allConnections = createConnections();
        this.dbServerUtilisation = getDbServerUtilisation();
    }

    public Map<String, T> getAllConnections() {
        return this.allConnections;
    }

    protected List<String> getDbServerQueue() {
        return this.dbServerUtilisation.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue())
                .map(entry -> entry.getKey())
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
        initialPopulateOfDbServerUtilisation();
        return this.dbServerUtilisation;
    }

    protected void trackUtilisation(String hostname) {
        this.dbServerUtilisation.computeIfPresent(hostname, (k, v) -> v+1);
    }

    private void initialPopulateOfDbServerUtilisation() {
        for (String newHostname : allConnections.keySet()) {
            dbServerUtilisation.putIfAbsent(newHostname, 0);
        }
    }

}
