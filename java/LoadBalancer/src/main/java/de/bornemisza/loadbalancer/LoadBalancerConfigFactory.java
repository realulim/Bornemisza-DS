package de.bornemisza.loadbalancer;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

public class LoadBalancerConfigFactory implements ObjectFactory {
    
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
                String srvRecordServiceName = (String) ref.get("service").getContent();
                String db = (String) ref.get("db").getContent();
                RefAddr userNameAddr = ref.get("username");
                String userName = userNameAddr == null ? null : (String) userNameAddr.getContent();
                RefAddr passwordAddr = ref.get("password");
                char[] password = passwordAddr == null ? null : ((String)passwordAddr.getContent()).toCharArray();
                return new LoadBalancerConfig(srvRecordServiceName, db, userName, password);
            }
            else {
                throw new NamingException("Expected Class: " + expectedClass + ", configured Class: " + refClassName);
            }
        }
    }

    private Class getExpectedClass() {
        return LoadBalancerConfig.class;
    }

}
