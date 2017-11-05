package de.bornemisza.loadbalancer;

public class ClusterEvent {

    private final String hostname;
    private final ClusterEventType type;

    public enum ClusterEventType {
        HOST_APPEARED, HOST_DISAPPEARED, HOST_HEALTHY, HOST_UNHEALTHY
    }

    public ClusterEvent(String hostname, ClusterEventType type) {
        this.hostname = hostname;
        this.type = type;
    }

    public String getHostname() {
        return hostname;
    }

    public ClusterEventType getType() {
        return type;
    }

}
