package de.bornemisza.loadbalancer.da;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.hazelcast.cache.ICache;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;

import de.bornemisza.loadbalancer.entity.SrvRecord;

public class DnsProvider {

    private final ICache<String, List<String>> cache;

    public DnsProvider(HazelcastInstance hazelcast) {
        ICacheManager cacheManager = hazelcast.getCacheManager();
        this.cache = cacheManager.getCache("DatabaseServers");
    }

    public List<String> getHostnamesForService(String service) throws NamingException {
        if (service == null) throw new IllegalArgumentException("Service is null!");
        if (cache.containsKey(service)) {
            return cache.get(service);
        }
        else {
            List<String> hostnames = getSrvRecordsSortedByPriority(service).stream()
                    .map(srvRecord -> srvRecord.getHost().replaceAll(".$", ""))
                    .collect(Collectors.toList());
            cache.put(service, hostnames, new CreatedExpiryPolicy(Duration.ONE_MINUTE));
            return hostnames;
        }
    }

    List<SrvRecord> getSrvRecordsSortedByPriority(String service) throws NamingException {
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

}
