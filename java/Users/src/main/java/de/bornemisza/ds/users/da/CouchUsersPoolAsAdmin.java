package de.bornemisza.ds.users.da;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.da.HttpPool;

@Stateless
public class CouchUsersPoolAsAdmin extends HttpPool {

    @Resource(name="lbconfig/CouchUsersAsAdmin")
    LoadBalancerConfig lbConfig;

    public CouchUsersPoolAsAdmin() {
        super();
    }

    @Override
    protected LoadBalancerConfig getLoadBalancerConfig() {
        return this.lbConfig;
    }

    public String getUserName() {
        return this.lbConfig.getUserName();
    }

    public char[] getPassword() {
        return this.lbConfig.getPassword();
    }

}
