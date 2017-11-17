package de.bornemisza.couchdb.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hazelcast.core.HazelcastInstance;

import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import de.bornemisza.couchdb.entity.CouchDbConnection;
import de.bornemisza.couchdb.entity.MyCouchDbConnector;
import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.loadbalancer.da.Pool;

public abstract class CouchPool extends Pool<CouchDbConnection> {

    private String serviceName;

    public CouchPool() {
        super();
    }

    // Constructor for Unit Tests
    public CouchPool(HazelcastInstance hz, DnsProvider dnsProvider, String serviceName) {
        super(hz, dnsProvider);
        this.serviceName = serviceName;
    }

    @Override
    protected Map<String, CouchDbConnection> createConnections() {
        LoadBalancerConfig lbConfig = getLoadBalancerConfig();
        this.serviceName = lbConfig.getServiceName();
        Map<String, CouchDbConnection> connections = new HashMap<>();
        for (String hostname : this.dnsProvider.getHostnamesForService(serviceName)) {
            CouchDbConnection conn = createConnection(lbConfig, hostname);
            connections.put(hostname, conn);
        }
        return connections;
    }

    @Override
    protected CouchDbConnection createConnection(LoadBalancerConfig lbConfig, String hostname) {
        try {
            String db = lbConfig.getInstanceName();
            String userName = lbConfig.getUserName();
            String password = lbConfig.getPassword() == null ? null : String.valueOf(lbConfig.getPassword());
            db = (db == null ? "" : db.replaceFirst ("^/*", ""));
            return new CouchDbConnection(new URL("https://" + hostname + "/"), db, userName, password);
        }
        catch (MalformedURLException ex) {
            throw new RuntimeException("Cannot create CouchDbConnection: " + ex.toString());
        }
    }

    public MyCouchDbConnector getConnector(String userName, char[] password) {
        List<String> dbServerQueue = getDbServerQueue();
        for (String hostname : dbServerQueue) {
            CouchDbConnection conn = allConnections.get(hostname);
            if (conn == null) {
                // in case a new SRV-Record popped up, but we haven't created a Connection for it yet
                conn = createConnection(getLoadBalancerConfig(), hostname);
                allConnections.put(hostname, conn);
            }
            HttpClient httpClient = createHttpClient(conn, userName, password);
            if (password != null) Arrays.fill(password, '*');
            CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
            trackUtilisation(hostname);
            return new MyCouchDbConnector(hostname, conn, dbInstance);
        }
        throw new IllegalStateException("No DbServer available at all!");
    }

    private HttpClient createHttpClient(CouchDbConnection conn, String userName, char[] password) {
        return new StdHttpClient.Builder()
                .url(conn.getBaseUrl())
                .username(userName)
                .password(String.valueOf(password))
                .build();
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

}
