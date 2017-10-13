package de.bornemisza.loadbalancer.da;

import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.hazelcast.core.HazelcastInstance;

public abstract class PoolFactory implements ObjectFactory {

    private final HazelcastInstance hazelcast;
    private final DnsProvider dnsProvider;

    public PoolFactory() throws NamingException {
        Context ctx = new InitialContext();
        this.hazelcast = (HazelcastInstance) ctx.lookup("payara/Hazelcast");
        if (hazelcast == null || !hazelcast.getLifecycleService().isRunning()) {
            throw new NamingException("Hazelcast not ready!");
        }
        this.dnsProvider = new DnsProvider(this.hazelcast);
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        // Logger.getAnonymousLogger().info("Getting Pool for " + name.toString());
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
                String service = (String) ref.get("service").getContent();
                String db = (String) ref.get("db").getContent();
                RefAddr userNameAddr = ref.get("username");
                String userName = userNameAddr == null ? null : (String) userNameAddr.getContent();
                RefAddr passwordAddr = ref.get("password");
                String password = passwordAddr == null ? null : (String) passwordAddr.getContent();
long start = System.currentTimeMillis();
                List<String> hostnames = this.dnsProvider.getHostnamesForService(service);
long duration = System.currentTimeMillis() - start;
Logger.getAnonymousLogger().info("Duration: " + duration);
                Object pool = createPool(hostnames, db, userName, password);
                return pool;
            }
            else {
                throw new NamingException("Expected Class: " + expectedClass + ", configured Class: " + refClassName);
            }
        }
    }

    protected abstract Object createPool(List<String> hostnames, String db, String userName, String password) throws NamingException, MalformedURLException;
    protected abstract Class getExpectedClass();

    protected HazelcastInstance getHazelcast() {
        return this.hazelcast;
    }

    protected DnsProvider getDnsProvider() {
        return this.dnsProvider;
    }

}
