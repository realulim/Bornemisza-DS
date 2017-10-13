package de.bornemisza.loadbalancer.da;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.loadbalancer.entity.SrvRecord;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

public class DnsProvider {

    private Cache<String, List<String>> cache;

    public DnsProvider(HazelcastInstance hazelcast) {
//        ICacheManager cacheManager = hazelcast.getCacheManager();
//        this.cache = cacheManager.getCache("DatabaseServers");
        CachingProvider cachingProvider = Caching.getCachingProvider();
        CacheManager cacheManager = cachingProvider.getCacheManager();
        List<String> myList = new ArrayList<>();
        CompleteConfiguration<String, List<String>> config = new MutableConfiguration<>();
        cache = cacheManager.createCache("DatabaseServers", config);
    }

    public static List<String> getHostnamesForService(String service) throws NamingException {
        if (service == null) throw new IllegalArgumentException("Service is null!");
        List<String> hostnames = getSrvRecordsSortedByPriority(service).stream()
                .map(srvRecord -> srvRecord.getHost().replaceAll(".$", ""))
                .collect(Collectors.toList());
        return hostnames;
    }

    static List<SrvRecord> getSrvRecordsSortedByPriority(String service) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        DirContext ctx = new InitialDirContext(env);
        return retrieveSrvRecordsAndSort(ctx, service);
    }

    static List<SrvRecord> retrieveSrvRecordsAndSort(DirContext ctx, String service) throws NamingException {
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
