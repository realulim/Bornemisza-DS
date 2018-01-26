package de.bornemisza.loadbalancer.da;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.strategy.LoadBalancerStrategy;
import de.bornemisza.loadbalancer.strategy.UtilisationStrategy;

public abstract class Pool<T> implements MessageListener<ClusterEvent> {

    @Inject
    protected HazelcastInstance hazelcast;

    private LoadBalancerStrategy lbStrategy;
    protected DnsProvider dnsProvider;
    protected Map<String, T> allConnections;
    private Set<String> candidates;

    private ITopic<ClusterEvent> clusterMaintenanceTopic;
    private String registrationId;

    protected abstract String getServiceName();
    protected abstract Map<String, T> createConnections();
    protected abstract T createConnection(LoadBalancerConfig lbConfig, String hostname);
    protected abstract LoadBalancerConfig getLoadBalancerConfig();

    public Pool() { }

    // Constructor for Unit Tests
    public Pool(HazelcastInstance hazelcast, LoadBalancerStrategy strategy, DnsProvider dnsProvider) {
        this.hazelcast = hazelcast;
        this.lbStrategy = strategy;
        this.dnsProvider = dnsProvider;
        this.initCluster();
    }

    @PostConstruct
    protected void init() {
        this.lbStrategy = new UtilisationStrategy(hazelcast);
        this.dnsProvider = new DnsProvider(hazelcast);
        this.initCluster();
    }

    private void initCluster() {
        this.allConnections = createConnections();
        this.candidates = hazelcast.getSet(Config.CANDIDATES);
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
                    if (! this.allConnections.containsKey(hostname)) {
                        T conn = createConnection(getLoadBalancerConfig(), hostname);
                        this.allConnections.put(hostname, conn);
                    }
                    this.candidates.remove(hostname);
                    break;
                case HOST_DISAPPEARED:
                    this.allConnections.remove(hostname);
                    Logger.getAnonymousLogger().info("Host " + hostname + " removed from service.");
                    break;
                case HOST_HEALTHY:
                    break;
                case HOST_UNHEALTHY:
                    break;
                default:
                    Logger.getAnonymousLogger().info("Unknown ClusterEvent: " + hostname + "/" + clusterEvent.getType().name());
            }
            this.lbStrategy.handleClusterEvent(clusterEvent);
        }
        catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, clusterEvent.toString(), e);
        }
    }

    public Map<String, T> getAllConnections() {
        return this.allConnections;
    }

    protected List<String> getDbServerQueue() {
        return lbStrategy.getHostQueue();
    }

    protected void trackUtilisation(String hostname) {
        this.lbStrategy.trackUsage(hostname);
    }

}
