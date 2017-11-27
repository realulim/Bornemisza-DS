package de.bornemisza.rest.security;

import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;

import de.bornemisza.loadbalancer.LoadBalancerConfig;

@Stateless
@Dependent
public class DbAdminPasswordBasedHashProvider extends HashProvider {

    private final LoadBalancerConfig lbConfig;
    
    public DbAdminPasswordBasedHashProvider(LoadBalancerConfig lbConfig) {
        super();
        this.lbConfig = lbConfig;
        super.init();
    }

    @Override
    protected char[] getServerSecret() {
        return lbConfig.getPassword();
    }

}
