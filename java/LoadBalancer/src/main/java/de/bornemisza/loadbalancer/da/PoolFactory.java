package de.bornemisza.loadbalancer.da;

import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.hazelcast.core.HazelcastInstance;
import java.util.logging.Logger;

public abstract class PoolFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
Logger.getAnonymousLogger().info("Getting Pool for " + getClass().getName());
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
                List<String> hostnames = DnsProvider.getHostnamesForService(service);
Logger.getAnonymousLogger().info("Got Pool");
                return createPool(hostnames, db, userName, password);
            }
            else {
                throw new NamingException("Expected Class: " + expectedClass + ", configured Class: " + refClassName);
            }
        }
    }

    protected abstract Object createPool(List<String> hostnames, String db, String userName, String password) throws NamingException, MalformedURLException;
    protected abstract Class getExpectedClass();

    protected HazelcastInstance getHazelcast() throws NamingException {
        Context ctx = new InitialContext();
        HazelcastInstance hazelcast = (HazelcastInstance) ctx.lookup("payara/Hazelcast");
        if (hazelcast == null || !hazelcast.getLifecycleService().isRunning()) {
            throw new NamingException("Hazelcast not ready!");
        }
        return hazelcast;
    }

}
