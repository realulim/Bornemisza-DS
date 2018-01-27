package de.bornemisza.rest.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import org.javalite.http.Http;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.loadbalancer.strategy.LoadBalancerStrategy;
import de.bornemisza.rest.HttpConnection;

public abstract class HttpPool extends Pool<HttpConnection> {

    private String serviceName;

    public HttpPool() {
        super();
    }

    // Constructor for Unit Tests
    public HttpPool(HazelcastInstance hz, LoadBalancerStrategy strategy, DnsProvider dnsProvider, String serviceName) {
        super(hz, strategy, dnsProvider);
        this.serviceName = serviceName;
    }

    @Override
    protected Map<String, HttpConnection> createConnections() {
        LoadBalancerConfig lbConfig = getLoadBalancerConfig();
        this.serviceName = lbConfig.getServiceName();
        Map<String, HttpConnection> connections = new HashMap<>();
        for (String hostname : this.dnsProvider.getHostnamesForService(serviceName)) {
            HttpConnection conn = createConnection(lbConfig, hostname);
            connections.put(hostname, conn);
        }
        return connections;
    }

    @Override
    protected HttpConnection createConnection(LoadBalancerConfig lbConfig, String hostname) {
        try {
            String db = lbConfig.getInstanceName();
            db = (db == null ? "" : db.replaceFirst ("^/*", ""));
            return new HttpConnection(db, new Http(new URL("https://" + hostname + "/" + db)));
        }
        catch (MalformedURLException ex) {
            throw new RuntimeException("Cannot create Http: " + ex.toString());
        }
    }

    /**
     * @return a load-balanced Connection (main API call for outside clients)
     */
    public HttpConnection getConnection() {
        String hostname = getNextDbServer();
        trackUtilisation(hostname);
        HttpConnection conn = allConnections.get(hostname);
        if (conn == null) {
            // Paranoia Fallback - should not happen
            conn = createConnection(getLoadBalancerConfig(), hostname);
            allConnections.put(hostname, conn);
            Logger.getAnonymousLogger().warning("Had to create emergency Connection for " + hostname);
        }
        return conn;
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

}
