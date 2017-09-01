package de.bornemisza.rest.da;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.spi.ObjectFactory;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.entity.SrvRecord;
import de.bornemisza.rest.HealthChecks;
import de.bornemisza.rest.Http;

public class HttpPoolFactory implements ObjectFactory {
    
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
            String expectedClass = HttpPool.class.getName();
            if (refClassName.equals(expectedClass)) {
                String service = (String) ref.get("service").getContent();
                String db = (String) ref.get("db").getContent();
                List<String> hostnames = getSrvRecordsSortedByPriority(service).stream()
                        .map(srvRecord -> srvRecord.getHost().replaceAll(".$", ""))
                        .collect(Collectors.toList());
                Map<String, Http> connections = new HashMap<>();
                for (String hostname : hostnames) {
                    Http conn = new Http(new URL("https://" + hostname + "/" + db));
                    connections.put(hostname, conn);
                }
                return new HttpPool(connections, getHazelcast(), new HealthChecks());
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

    private HazelcastInstance getHazelcast() throws NamingException {
        Context ctx = new InitialContext();
        HazelcastInstance hazelcast = (HazelcastInstance) ctx.lookup("payara/Hazelcast");
        if (hazelcast == null || !hazelcast.getLifecycleService().isRunning()) {
            throw new NamingException("Hazelcast not ready!");
        }
        return hazelcast;
    }

}
