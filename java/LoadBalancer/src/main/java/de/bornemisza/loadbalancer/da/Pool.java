package de.bornemisza.loadbalancer.da;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.LoadBalancerConfig;

public abstract class Pool<T> implements MessageListener<ClusterEvent> {

    @Inject
    protected HazelcastInstance hazelcast;

    protected DnsProvider dnsProvider;
    protected Map<String, T> allConnections;

    private Map<String, Integer> dbServerUtilisation = null;
    private ITopic<ClusterEvent> clusterMaintenanceTopic;
    private String registrationId;

    protected abstract String getServiceName();
    protected abstract Map<String, T> createConnections();
    protected abstract T createConnection(LoadBalancerConfig lbConfig, String hostname);
    protected abstract LoadBalancerConfig getLoadBalancerConfig();

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
        this.clusterMaintenanceTopic = hazelcast.getReliableTopic(Config.TOPIC_CLUSTER_MAINTENANCE);
        this.registrationId = clusterMaintenanceTopic.addMessageListener(this);
    }

    @PreDestroy
    public void dispose() {
        clusterMaintenanceTopic.removeMessageListener(registrationId);
        Logger.getAnonymousLogger().info("Message Listener " + registrationId + " removed.");
    }

    @Override
    public void onMessage(Message<ClusterEvent> msg) {
        ClusterEvent clusterEvent = msg.getMessageObject();
        String hostname = clusterEvent.getHostname();
        switch (clusterEvent.getType()) {
            case HOST_APPEARED:
                if (! this.dbServerUtilisation.containsKey(hostname)) {
                    this.dbServerUtilisation.put(hostname, 0);
                }
                if (! this.allConnections.containsKey(hostname)) {
                    this.allConnections.put(hostname, createConnection(getLoadBalancerConfig(), hostname));
                    resetUtilisation(); // start everyone on equal terms
                }
                break;
            case HOST_DISAPPEARED:
                if (this.dbServerUtilisation.containsKey(hostname)) {
                    this.dbServerUtilisation.remove(hostname);
                }
                if (this.allConnections.containsKey(hostname)) {
                    this.allConnections.remove(hostname);
                }
                break;
            case HOST_HEALTHY:
                resetUtilisation(); // start everyone on equal terms
                this.dbServerUtilisation.put(hostname, 0);
                break;
            case HOST_UNHEALTHY:
                this.dbServerUtilisation.remove(hostname);
                break;
            default:
        }
        Logger.getAnonymousLogger().info("ClusterEvent: " + hostname + "/" + clusterEvent.getType().name());
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

    private void resetUtilisation() {
        for (String hostname : this.dbServerUtilisation.keySet()) {
            this.dbServerUtilisation.put(hostname, 0);
        }
    }

}
