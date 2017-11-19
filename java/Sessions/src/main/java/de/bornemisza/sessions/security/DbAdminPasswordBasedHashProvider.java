package de.bornemisza.sessions.security;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.security.HashProvider;

@Stateless
public class DbAdminPasswordBasedHashProvider extends HashProvider {
    
    @Resource(name="lbconfig/CouchUsersAsAdmin")
    LoadBalancerConfig lbConfig;

    public DbAdminPasswordBasedHashProvider() {
        super();
    }

    // Constructor for Unit Tests
    public DbAdminPasswordBasedHashProvider(LoadBalancerConfig lbConfig) {
        this.lbConfig = lbConfig;
        super.init();
    }

    @Override
    protected char[] getServerSecret() {
        return lbConfig.getPassword();
    }

}
