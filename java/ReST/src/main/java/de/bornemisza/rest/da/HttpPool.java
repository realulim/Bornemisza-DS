package de.bornemisza.rest.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.rest.Http;

public abstract class HttpPool extends Pool<Http> {

    private String serviceName;

    public HttpPool() {
        super();
    }

    // Constructor for Unit Tests
    public HttpPool(HazelcastInstance hz, DnsProvider dnsProvider, String serviceName) {
        super(hz, dnsProvider);
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

    @Override
    protected Http createConnection(LoadBalancerConfig lbConfig, String hostname) {
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
        for (String hostname : getDbServerQueue()) {
            Http conn = allConnections.get(hostname);
            if (conn == null) {
                // in case a new SRV-Record popped up, but we haven't created a Connection for it yet
                conn = createConnection(getLoadBalancerConfig(), hostname);
                allConnections.put(hostname, conn);
                Logger.getAnonymousLogger().info("Created new Connection for " + hostname);
            }
            trackUtilisation(hostname);
            return conn;
        }
        throw new IllegalStateException("No DbServer available at all!");
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

}
