package de.bornemisza.rest.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.hazelcast.core.HazelcastInstance;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.loadbalancer.da.Pool;
import de.bornemisza.rest.HttpConnection;

public abstract class HttpPool extends Pool<HttpConnection> {

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
        List<String> dbServerQueue = getDbServerQueue();
        if (dbServerQueue.isEmpty()) {
            // Paranoia Fallback - if no server seems to be healthy, we'll try one of the existing connections
            for (HttpConnection conn : getAllConnections().values()) {
                if (isConnectionHealthy(conn)) return conn;
            }
        }
        else {
            String hostname = dbServerQueue.get(0);
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
        throw new IllegalStateException("No DbServer available at all!");
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

    private boolean isConnectionHealthy(HttpConnection conn) {
        Get get = conn.getHttp().get("", 200, 2000);
        try {
            if (get.responseCode() == 200) {
                return true;
            }
        }
        catch (HttpException ex) {
            return false;
        }
        return false;
    }

}
