package de.bornemisza.loadbalancer.strategy;

import java.util.List;

import de.bornemisza.loadbalancer.ClusterEvent;

public interface LoadBalancerStrategy {

    /**
     * Method is called by a client whenever he wants to know which host should be used next.
     * 
     * @return the fully qualified hostname of the next host to be used
     */
    String getNextHost();

    /**
     * Method is called by a client whenever he wants to know which hosts are going to be used next.
     * The queue can contain all available hosts in planned usage order or only the first few.
     * 
     * @return a list of fully qualified hostnames
     */
    List<String> getHostQueue();

    /**
     * Method is called whenever a client is done handling a cluster event, so the LoadBalancerStrategy gets a chance to look at it as well.
     * 
     * @param event the Cluster Event handled by the client
     */
    void handleClusterEvent(ClusterEvent event);

    /**
     * Method is called by a client, who wants to signal that he is right now using this particular host.
     * Implementations may wish to track usage for balancing requests or gathering statistics, but the
     * method body can also remain empty.
     * 
     * @param hostname the fully qualified hostname of the host that is being used right now
     */
    void trackUsage(String hostname);

}
