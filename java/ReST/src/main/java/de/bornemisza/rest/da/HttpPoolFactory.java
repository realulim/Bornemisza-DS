package de.bornemisza.rest.da;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import de.bornemisza.loadbalancer.da.PoolFactory;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;
import java.util.logging.Logger;

public class HttpPoolFactory extends PoolFactory {

    public HttpPoolFactory() throws NamingException {
        super();
    }

    @Override
    protected Object createPool(List<String> hostnames, String db, String userName, String password) throws MalformedURLException {
        Map<String, Http> connections = new HashMap<>();
        db = (db == null ? "" : db.replaceFirst ("^/*", ""));
        for (String hostname : hostnames) {
if (hostname == null) Logger.getAnonymousLogger().warning("Hostname null!!!!!!");
            Http conn = new Http(new URL("https://" + hostname + "/" + db));
            connections.put(hostname, conn);
        }
        return new HttpPool(connections, getHazelcast(), new HealthChecks());
    }

    @Override
    protected Class getExpectedClass() {
        return HttpPool.class;
    }

}
