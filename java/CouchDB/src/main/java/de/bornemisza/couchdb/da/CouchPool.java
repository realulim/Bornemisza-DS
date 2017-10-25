package de.bornemisza.couchdb.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.NamingException;

import com.hazelcast.core.HazelcastInstance;

import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import de.bornemisza.couchdb.HealthChecks;
import de.bornemisza.couchdb.entity.CouchDbConnection;
import de.bornemisza.couchdb.entity.MyCouchDbConnector;
import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.loadbalancer.da.DnsProvider;
import de.bornemisza.loadbalancer.da.Pool;

public abstract class CouchPool extends Pool<CouchDbConnection> {

    private final HealthChecks healthChecks;
    private String serviceName;

    public CouchPool() {
        super();
        this.healthChecks = new HealthChecks();
    }

    // Constructor for Unit Tests
    public CouchPool(HazelcastInstance hz, DnsProvider dnsProvider, HealthChecks healthChecks, String serviceName) {
        super(hz, dnsProvider);
        this.healthChecks = healthChecks;
        this.serviceName = serviceName;
    }

    protected abstract LoadBalancerConfig getLoadBalancerConfig();

    @Override
    protected Map<String, CouchDbConnection> createConnections() {
        LoadBalancerConfig lbConfig = getLoadBalancerConfig();
        this.serviceName = lbConfig.getServiceName();
        Map<String, CouchDbConnection> connections = new HashMap<>();
        String db = lbConfig.getInstanceName();
        String userName = lbConfig.getUserName();
        String password = lbConfig.getPassword() == null ? null : String.valueOf(lbConfig.getPassword());
        db = (db == null ? "" : db.replaceFirst ("^/*", ""));
        try {
            for (String hostname : this.dnsProvider.getHostnamesForService(serviceName)) {
                CouchDbConnection conn = new CouchDbConnection(new URL("https://" + hostname + "/"), db, userName, password);
                connections.put(hostname, conn);
            }
        }
        catch (MalformedURLException ex) {
            throw new RuntimeException("Cannot create CouchPool: " + ex.toString());
        }
        return connections;
    }

    public MyCouchDbConnector getConnector() {
        return getConnector(null, null);
    }

    public MyCouchDbConnector getConnector(String userName, char[] password) {
        List<String> dbServerQueue = getDbServerQueue();
        for (String hostname : dbServerQueue) {
            CouchDbConnection conn = allConnections.get(hostname);
            if (healthChecks.isCouchDbReady(conn)) {
                Logger.getAnonymousLogger().fine(hostname + " available, using it.");
                HttpClient httpClient = createHttpClient(conn, userName, password);
                if (password != null) Arrays.fill(password, '*');
                CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
                trackUtilisation(hostname);
                return new MyCouchDbConnector(hostname, conn, dbInstance);
            }
            else {
                Logger.getAnonymousLogger().info(hostname + " unreachable...");
            }
        }
        Logger.getAnonymousLogger().warning("No CouchDB Hosts available at all!");
        throw new DbAccessException("No CouchDB Backend ready!");
    }

    private HttpClient createHttpClient(CouchDbConnection conn, String userName, char[] password) {
        if (userName == null) return new StdHttpClient.Builder()
                    .url(conn.getBaseUrl())
                    .username(conn.getUserName())
                    .password(conn.getPassword())
                    .build();
        else return new StdHttpClient.Builder()
                    .url(conn.getBaseUrl())
                    .username(userName)
                    .password(String.valueOf(password))
                    .build();
    }

    @Override
    public String getServiceName() {
        return this.serviceName;    }

}
