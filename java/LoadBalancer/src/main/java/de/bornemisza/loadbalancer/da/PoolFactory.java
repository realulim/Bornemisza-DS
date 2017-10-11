package de.bornemisza.loadbalancer.da;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.spi.ObjectFactory;

import com.hazelcast.core.HazelcastInstance;
import de.bornemisza.loadbalancer.Config;

import de.bornemisza.loadbalancer.entity.SrvRecord;

public abstract class PoolFactory implements ObjectFactory {

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
            String expectedClass = getClass().getName();
            if (refClassName.equals(getExpectedClass().getName())) {
                Config.DBSERVICE = (String) ref.get("service").getContent();
                String db = (String) ref.get("db").getContent();
                RefAddr userNameAddr = ref.get("username");
                String userName = userNameAddr == null ? null : (String) userNameAddr.getContent();
                RefAddr passwordAddr = ref.get("password");
                String password = passwordAddr == null ? null : (String) passwordAddr.getContent();
                List<String> hostnames = getHostnamesForService(Config.DBSERVICE);
                return createPool(hostnames, db, userName, password);
            }
            else {
                throw new NamingException("Expected Class: " + expectedClass + ", configured Class: " + refClassName);
            }
        }
    }

    protected abstract Object createPool(List<String> hostnames, String db, String userName, String password) throws NamingException, MalformedURLException;
    protected abstract Class getExpectedClass();

    public static List<String> getHostnamesForService(String service) throws NamingException {
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

    protected HazelcastInstance getHazelcast() throws NamingException {
        Context ctx = new InitialContext();
        HazelcastInstance hazelcast = (HazelcastInstance) ctx.lookup("payara/Hazelcast");
        if (hazelcast == null || !hazelcast.getLifecycleService().isRunning()) {
            throw new NamingException("Hazelcast not ready!");
        }
        return hazelcast;
    }

}
