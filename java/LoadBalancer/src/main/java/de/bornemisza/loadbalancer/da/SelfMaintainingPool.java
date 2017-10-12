package de.bornemisza.loadbalancer.da;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.hazelcast.core.HazelcastInstance;

public abstract class SelfMaintainingPool<T> extends Pool<T> {

    private static long TIMESTAMP = System.currentTimeMillis();
    private static final int PERIOD_IN_MILLIS = 10000;

    public SelfMaintainingPool(Map<String, T> allConnections, HazelcastInstance hazelcast) {
        super(allConnections, hazelcast);
    }

    public void performMaintenance() {
        if ((System.currentTimeMillis() - PERIOD_IN_MILLIS) > TIMESTAMP) {
            TIMESTAMP = System.currentTimeMillis();
            dbServerQueueLocal = sortHostnamesByUtilisation();
            logNewQueueState();
        }
    }

    List<String> sortHostnamesByUtilisation() {
        return this.getDbServerUtilisation().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue())
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    private void logNewQueueState() {
        StringBuilder sb = new StringBuilder("DbServerQueue");
        for (String hostname : dbServerQueueLocal) {
            sb.append(" | ").append(hostname).append(":").append(this.dbServerUtilisation.get(hostname));
        }
        Logger.getAnonymousLogger().info(sb.toString());
    }

}
