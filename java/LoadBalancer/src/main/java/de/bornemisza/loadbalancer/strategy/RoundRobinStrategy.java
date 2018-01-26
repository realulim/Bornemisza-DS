package de.bornemisza.loadbalancer.strategy;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.ClusterEvent;

/**
 * A Load Balancer Strategy where hosts take turns in round robin format.
 */
public class RoundRobinStrategy extends UtilisationStrategy {

    private final LinkedList<String> hosts;

    public RoundRobinStrategy(HazelcastInstance hazelcast) {
        super(hazelcast);
        this.hosts = new LinkedList<>();
        this.hosts.addAll(hostUtilisation.keySet());
    }

    @Override
    public String getNextHost() {
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
        String hostname = clusterEvent.getHostname();
        try {
            switch (clusterEvent.getType()) {
                case CANDIDATE_HEALTHY:
                    if (! this.hosts.contains(hostname)) {
                        this.hosts.add(hostname);
                        Logger.getAnonymousLogger().info("Candidate " + hostname + " promoted into rotation.");
                    }
                    break;
                case HOST_DISAPPEARED:
                    this.hosts.remove(hostname);
                    break;
                case HOST_HEALTHY:
                    if (! this.hosts.contains(hostname)) {
                        this.hosts.add(hostname);
                        Logger.getAnonymousLogger().info("Put host " + hostname + " back into rotation.");
                    }
                    break;
                case HOST_UNHEALTHY:
                    boolean removed = this.hosts.remove(hostname);
                    if (removed) Logger.getAnonymousLogger().info("Host " + hostname + " taken out of rotation.");
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
