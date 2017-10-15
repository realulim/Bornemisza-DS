package de.bornemisza.couchdb.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import de.bornemisza.couchdb.HealthChecks;
import de.bornemisza.couchdb.entity.CouchDbConnection;
import de.bornemisza.loadbalancer.da.PoolFactory;

public class ConnectionPoolFactory extends PoolFactory {

    public ConnectionPoolFactory() throws NamingException {
        super();
    }

    @Override
    public Object createPool(String srvRecordServiceName, String db, String userName, String password) throws MalformedURLException, NamingException {
        Map<String, CouchDbConnection> connections = new HashMap<>();
        db = (db == null ? "" : db.replaceFirst ("^/*", ""));
        for (String hostname : this.dnsProvider.getHostnamesForService(srvRecordServiceName)) {
            CouchDbConnection conn = new CouchDbConnection(new URL("https://" + hostname + "/"), db, userName, password);
            connections.put(hostname, conn);
        }
        return new ConnectionPool(connections, this.hazelcast, new HealthChecks());
    }

    @Override
    protected Class getExpectedClass() {
        return ConnectionPool.class;
    }

}
