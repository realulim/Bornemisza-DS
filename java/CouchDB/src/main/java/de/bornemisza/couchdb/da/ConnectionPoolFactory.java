package de.bornemisza.couchdb.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import de.bornemisza.couchdb.HealthChecks;
import de.bornemisza.couchdb.entity.CouchDbConnection;
import de.bornemisza.loadbalancer.da.PoolFactory;

public class ConnectionPoolFactory extends PoolFactory {

    @Override
    public Object createPool(List<String> hostnames, String db, String userName, String password) throws NamingException, MalformedURLException {
        Map<String, CouchDbConnection> connections = new HashMap<>();
        db = (db == null ? "" : db.replaceFirst ("^/*", ""));
        for (String hostname : hostnames) {
            CouchDbConnection conn = new CouchDbConnection(new URL("https://" + hostname + "/"), db, userName, password);
            connections.put(hostname, conn);
        }
        return new ConnectionPool(connections, getHazelcast(), new HealthChecks());
    }

    @Override
    protected Class getExpectedClass() {
        return ConnectionPool.class;
    }

}
