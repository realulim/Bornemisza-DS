package de.bornemisza.rest.da;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import de.bornemisza.loadbalancer.Config;
import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;

public class HttpPool extends Pool<Http> {

    private final HealthChecks healthChecks;
    private final ITopic<List<String>> databaseServersTopic;
    private final String registrationId;

    public HttpPool(Map<String, Http> allConnections,
                    HazelcastInstance hazelcast,
                    HealthChecks healthChecks) {
        super(allConnections, hazelcast);
        this.healthChecks = healthChecks;
        this.databaseServersTopic = hazelcast.getReliableTopic(Config.DATABASE_SERVERS_TOPIC);
        this.registrationId = this.databaseServersTopic.addMessageListener(new DatabaseServersListener());
    }

    public Http getConnection() {
        List<String> dbServerQueue = getDbServerQueue();
        for (String hostname : dbServerQueue) {
            Http conn = allConnections.get(hostname);
            if (healthChecks.isCouchDbReady(conn)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
                trackUtilisation(hostname);
                return conn;
            }
            else {
                Logger.getAnonymousLogger().info(hostname + " unreachable...");
            }
        }
        Logger.getAnonymousLogger().warning("No CouchDB Hosts available at all!");
        throw new RuntimeException("No CouchDB Backend ready!");
    }

    @PreDestroy
    public void dispose() {
        this.databaseServersTopic.removeMessageListener(registrationId);
        Logger.getAnonymousLogger().info("Message Listener " + registrationId + " removed.");
    }

    class DatabaseServersListener implements MessageListener<List<String>> {
        @Override public void onMessage(Message<List<String>> msg) {
            List<String> publishedHostnames = msg.getMessageObject();
            Set<String> currentHostnames = allConnections.keySet();
            for (String publishedHostname : publishedHostnames) {
                if (! currentHostnames.contains(publishedHostname)) {
                    Logger.getAnonymousLogger().info("New Database Server: " + publishedHostname);
                }
            }
            for (String currentHostname : currentHostnames) {
                if (! publishedHostnames.contains(currentHostname)) {
                    Logger.getAnonymousLogger().info("Ex Database Server: " + currentHostname);
                }
            }
        }
    }

}
