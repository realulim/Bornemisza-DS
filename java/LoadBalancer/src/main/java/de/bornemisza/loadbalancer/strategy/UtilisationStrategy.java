package de.bornemisza.loadbalancer.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import de.bornemisza.loadbalancer.ClusterEvent;
import de.bornemisza.loadbalancer.Config;

/**
 * A Load Balancer Strategy that tracks host usage and gives preference to those less used.
 */
public class UtilisationStrategy implements LoadBalancerStrategy {

    private final HazelcastInstance hazelcast;

    private Map<String, Integer> hostUtilisation = null;

    public UtilisationStrategy(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
        this.hostUtilisation = getHostUtilisation();
    }

    @Override
    public String getNextHost() {
        return getHostQueue().get(0);
    }

    @Override
    public List<String> getHostQueue() {
        return this.hostUtilisation.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue())
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    @Override
    public void handleClusterEvent(ClusterEvent clusterEvent) {
        String hostname = clusterEvent.getHostname();
        try {
            switch (clusterEvent.getType()) {
                case CANDIDATE_HEALTHY:
                    if (! this.hostUtilisation.containsKey(hostname)) {
                        resetUtilisation(); // start everyone on equal terms
                        this.hostUtilisation.put(hostname, 0);
                        Logger.getAnonymousLogger().info("Candidate " + hostname + " promoted into rotation.");
                    }
                    break;
                case HOST_DISAPPEARED:
                    this.hostUtilisation.remove(hostname);
                    break;
                case HOST_HEALTHY:
                    if (! this.hostUtilisation.containsKey(hostname)) {
                        resetUtilisation(); // start everyone on equal terms
                        this.hostUtilisation.put(hostname, 0);
                        Logger.getAnonymousLogger().info("Put host " + hostname + " back into rotation.");
                    }
                    break;
                case HOST_UNHEALTHY:
                    Integer count = this.hostUtilisation.remove(hostname);
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

    @Override
    public void trackUsage(String hostname) {
        this.hostUtilisation.computeIfPresent(hostname, (k, v) -> v+1);
    }
    
    Map<String, Integer> getHostUtilisation() {
        if (this.hostUtilisation != null && this.hostUtilisation instanceof IMap) {
            // It's a Hazelcast map, so all is good
            return this.hostUtilisation;
        }
        // Not a Hazelcast map, so let's try to make it one
        try {
            this.hostUtilisation = hazelcast.getMap(Config.UTILISATION);
        }
        catch (HazelcastException e) {
            // no Hazelcast, so let's make it a plain map and try again next time
            Logger.getAnonymousLogger().warning("Hazelcast malfunctioning: " + e.toString());
            this.hostUtilisation = new HashMap<>(); // fallback, so clients can still work
        }
        return this.hostUtilisation;
    }

    private void resetUtilisation() {
        Set<String> hostnames = this.hostUtilisation.keySet();
        for (String hostname : hostnames) {
            this.hostUtilisation.put(hostname, 0);
        }
        Logger.getAnonymousLogger().info("Utilisation reset for " + hostnames);
    }

}
