package de.bornemisza.loadbalancer.da;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
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
    private Set<String> candidates;

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
        this.candidates = hazelcast.getSet(Config.CANDIDATES);
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
        try {
            switch (clusterEvent.getType()) {
                case CANDIDATE_HEALTHY:
                    // promote candidate into rotation
                    T conn = createConnection(getLoadBalancerConfig(), hostname);
                    this.allConnections.put(hostname, conn);
                    if (! this.dbServerUtilisation.containsKey(hostname)) {
                        resetUtilisation(); // start everyone on equal terms
                        // Caution: getConnection() can create emergency connections, which can lead to a race condition here
                        this.dbServerUtilisation.put(hostname, 0);
                    }
                    this.candidates.remove(hostname);
                    Logger.getAnonymousLogger().info("Candidate " + hostname + " promoted into rotation.");
                    break;
                case HOST_DISAPPEARED:
                    this.dbServerUtilisation.remove(hostname);
                    this.allConnections.remove(hostname);
                    Logger.getAnonymousLogger().info("Host " + hostname + " removed from pool.");
                    break;
                case HOST_HEALTHY:
                    if (! this.dbServerUtilisation.containsKey(hostname)) {
                        resetUtilisation(); // start everyone on equal terms
                        this.dbServerUtilisation.put(hostname, 0);
                        Logger.getAnonymousLogger().info("Put host " + hostname + " back into rotation.");
                    }
                    break;
                case HOST_UNHEALTHY:
                    Integer count = this.dbServerUtilisation.remove(hostname);
                    if (count != null) Logger.getAnonymousLogger().info("Host " + hostname + " taken out of rotation.");
                    break;
                default:
                    Logger.getAnonymousLogger().info("Unknown ClusterEvent: " + hostname + "/" + clusterEvent.getType().name());
            }
        }
        catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, clusterEvent.toString(), e);
        }
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
        Set<String> hostnames = this.dbServerUtilisation.keySet();
        for (String hostname : hostnames) {
            this.dbServerUtilisation.put(hostname, 0);
        }
        Logger.getAnonymousLogger().info("Utilisation reset for " + hostnames);
    }

}
