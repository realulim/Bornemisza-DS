package de.bornemisza.ds.loadbalancer.strategy;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.ds.loadbalancer.ClusterEvent;

/**
 * A Load Balancer Strategy where hosts take turns in round robin format.
 */
public class RoundRobinStrategy extends UtilisationStrategy {

    private final LinkedList<String> hosts = new LinkedList<>();

    public RoundRobinStrategy(HazelcastInstance hazelcast) {
        super(hazelcast);
        this.init();
    }

    private void init() {
        this.hosts.addAll(getHostUtilisation().keySet());
        Collections.sort(hosts);
        Logger.getAnonymousLogger().info("Starting Round Robin Strategy with " + this.hosts);
    }

    @Override
    public String getNextHost() {
        if (hosts.isEmpty()) return null;
        String host = hosts.removeFirst();
        hosts.addLast(host);
        return host;
    }

    @Override
    public List<String> getHostQueue() {
        return hosts;
    }

    @Override
    public void handleClusterEvent(ClusterEvent clusterEvent) {
        super.handleClusterEvent(clusterEvent);
        String hostname = clusterEvent.getHostname();
        try {
            switch (clusterEvent.getType()) {
                case CANDIDATE_HEALTHY:
                    if (! this.hosts.contains(hostname)) {
                        this.hosts.add(hostname);
                    }
                    break;
                case HOST_DISAPPEARED:
                    this.hosts.remove(hostname);
                    break;
                case HOST_HEALTHY:
                    if (! this.hosts.contains(hostname)) {
                        this.hosts.add(hostname);
                    }
                    break;
                case HOST_UNHEALTHY:
                    this.hosts.remove(hostname);
                    break;
                default:
                    Logger.getAnonymousLogger().info("Unknown ClusterEvent: " + hostname + "/" + clusterEvent.getType().name());
            }
        }
        catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, clusterEvent.toString(), e);
        }
    }

}
