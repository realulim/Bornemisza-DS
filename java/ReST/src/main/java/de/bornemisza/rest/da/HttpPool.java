package de.bornemisza.rest.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;

public abstract class HttpPool extends Pool<Http> {

    private final HealthChecks healthChecks;
    private String serviceName;

    public HttpPool() {
        super();
        this.healthChecks = new HealthChecks();
    }

    protected abstract LoadBalancerConfig getLoadBalancerConfig();

    // Constructor for Unit Tests
    public HttpPool(HazelcastInstance hz, DnsProvider dnsProvider, HealthChecks healthChecks, String serviceName) {
        super(hz, dnsProvider);
        this.healthChecks = healthChecks;
        this.serviceName = serviceName;
    }

    @Override
    protected Map<String, Http> createConnections() {
        LoadBalancerConfig lbConfig = getLoadBalancerConfig();
        this.serviceName = lbConfig.getServiceName();
        Map<String, Http> connections = new HashMap<>();
        for (String hostname : this.dnsProvider.getHostnamesForService(serviceName)) {
            Http conn = createConnection(lbConfig, hostname);
            connections.put(hostname, conn);
        }
        return connections;
    }

    private Http createConnection(LoadBalancerConfig lbConfig, String hostname) {
        try {
            String db = lbConfig.getInstanceName();
            db = (db == null ? "" : db.replaceFirst ("^/*", ""));
            return new Http(new URL("https://" + hostname + "/" + db));
        }
        catch (MalformedURLException ex) {
            throw new RuntimeException("Cannot create Http: " + ex.toString());
        }
    }

    public Http getConnection() {
        List<String> dbServerQueue = getDbServerQueue();
        for (String hostname : dbServerQueue) {
            Http conn = allConnections.get(hostname);
            if (conn == null) {
                // in case a new SRV-Record popped up, but we haven't created a Connection for it yet
                conn = createConnection(getLoadBalancerConfig(), hostname);
                allConnections.put(hostname, conn);
            }
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

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

}
