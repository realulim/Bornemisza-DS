package de.bornemisza.loadbalancer.da;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
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
import javax.naming.ConfigurationException;
import javax.naming.directory.Attribute;

public class DnsProvider {

    private final HazelcastInstance hazelcast;
    private final ICache<String, List<String>> cache;
    private List<String> cachedDatabaseServers = new ArrayList<>();

    private final Hashtable<String, String> env = new Hashtable<>();
    private DirContext ctx;

    public DnsProvider(HazelcastInstance hz) {
        this.hazelcast = hz;
        ICacheManager cacheManager = hazelcast.getCacheManager();
        this.cache = cacheManager.getCache("DatabaseServers");

        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns://" + System.getProperty("DNSRESOLVER") + "/");
        Logger.getAnonymousLogger().info("Using DNS Resolver: " + env.get("java.naming.provider.url"));
    }

    // Constructor for Unit Tests
    public DnsProvider(HazelcastInstance hazelcast, DirContext context) {
        this(hazelcast);
        this.ctx = context;
    }

    public List<String> getHostnamesForService(String service) {
        List<String> hostnames = cache.get(service);
        if (hostnames != null) return hostnames; // cache hit
        else {
            try {
                // Read SRV-Records from DNS
                hostnames = retrieveSrvRecordsAndSort(service).stream()
                        .map(srvRecord -> srvRecord.getHost().replaceAll(".$", ""))
                        .collect(Collectors.toList());
                this.cachedDatabaseServers = hostnames; // cache locally as a fallback in case of DNS failures
                cache.put(service, hostnames, new CreatedExpiryPolicy(Duration.ONE_MINUTE));
                Logger.getAnonymousLogger().info("Refreshing SRV-Record Cache: " + String.join(",", hostnames));
            }
            catch (NamingException ex) {
                Logger.getAnonymousLogger().warning("Problem getting SRV-Records: " + ex.toString());
                return this.cachedDatabaseServers;
            }
            return hostnames;
        }
    }

    List<SrvRecord> retrieveSrvRecordsAndSort(String service) throws NamingException {
        if (ctx == null) ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes(service, new String[] {"SRV"});
        Attribute att = attrs.get("srv");
        if (att == null) throw new NamingException("Malformed SRV-Record: " + attrs.toString());
        NamingEnumeration<?> servers = att.getAll();
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
