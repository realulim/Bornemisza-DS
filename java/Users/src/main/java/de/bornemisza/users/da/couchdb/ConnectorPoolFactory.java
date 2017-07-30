package de.bornemisza.users.da.couchdb;

import com.hazelcast.core.HazelcastInstance;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.spi.ObjectFactory;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import de.bornemisza.users.entity.SrvRecord;
import java.util.stream.Collectors;
import javax.naming.InitialContext;

public class ConnectorPoolFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        if (obj == null) {
            throw new NamingException("Reference is null");
        }
        else if (!(obj instanceof Reference)) {
            throw new NamingException("No Reference: " + obj.getClass().getName());
        }
        else {
            Reference ref = (Reference) obj;
            String refClassName = ref.getClassName();
            String expectedClass = ConnectorPool.class.getName();
            if (refClassName.equals(expectedClass)) {
                String service = (String) ref.get("service").getContent();
                String db = (String) ref.get("db").getContent();
                String userName = (String) ref.get("username").getContent();
                String password = (String) ref.get("password").getContent();
                List<String> hostnames = getSrvRecordsSortedByPriority(service).stream()
                        .map(srvRecord -> srvRecord.getHost().replaceAll(".$", ""))
                        .collect(Collectors.toList());
                Map<String, CouchDbConnector> connectors = new HashMap<>();
                for (String hostname : hostnames) {
                    HttpClient httpClient = createHttpClient(hostname, db, userName, password);
                    CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
                    connectors.put(hostname, new StdCouchDbConnector(db, dbInstance));
                }
                return new ConnectorPool(connectors, getHazelcast(), new HealthChecks());
            }
            else {
                throw new NamingException("Expected Class: " + expectedClass + ", configured Class: " + refClassName);
            }
        }
    }

    private List<SrvRecord> getSrvRecordsSortedByPriority(String service) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        DirContext ctx = new InitialDirContext(env);
        return retrieveSrvRecordsAndSort(ctx, service);
    }

    List<SrvRecord> retrieveSrvRecordsAndSort(DirContext ctx, String service) throws NamingException {
        Attributes attrs = ctx.getAttributes(service, new String[] {"SRV"});
        NamingEnumeration<?> servers = attrs.get("srv").getAll();
        Set<SrvRecord> sortedRecords = new TreeSet<>();
        while (servers.hasMore()) {
            SrvRecord record = SrvRecord.fromString((String) servers.next());
            sortedRecords.add(record);
        }
        if (sortedRecords.isEmpty()) {
            throw new NamingException("No Service " + service + " found!");
        }
        else {
            return new ArrayList<>(sortedRecords);
        }
    }

    private HttpClient createHttpClient(String hostname, String db, String userName, String password) throws MalformedURLException {
        return new StdHttpClient.Builder()
                .url("https://" + hostname + "/" + db)
                .username(userName).password(password)
                .build();
    }

    private HazelcastInstance getHazelcast() throws NamingException {
        Context ctx = new InitialContext();
        HazelcastInstance hazelcast = (HazelcastInstance) ctx.lookup("payara/Hazelcast");
        if (hazelcast == null || !hazelcast.getLifecycleService().isRunning()) {
            throw new NamingException("Hazelcast not ready!");
        }
        return hazelcast;
    }

}
