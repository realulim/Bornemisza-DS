package de.bornemisza.ds.loadbalancer;

import java.io.Serializable;
import java.util.Objects;

public class ClusterEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String hostname;
    private final ClusterEventType type;

    public enum ClusterEventType {
        CANDIDATE_APPEARED, CANDIDATE_HEALTHY, HOST_DISAPPEARED, HOST_HEALTHY, HOST_UNHEALTHY
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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Objects.hashCode(this.hostname);
        hash = 73 * hash + Objects.hashCode(this.type);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ClusterEvent other = (ClusterEvent) obj;
        if (!Objects.equals(this.hostname, other.hostname)) {
            return false;
        }
        return this.type == other.type;
    }

    @Override
    public String toString() {
        return "ClusterEvent{" + "hostname=" + hostname + ", type=" + type + '}';
    }

}
