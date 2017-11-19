package de.bornemisza.users.da;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.da.HttpPool;

@Stateless
public class CouchUsersPool extends HttpPool {

    @Resource(name = "lbconfig/CouchUsers")
    LoadBalancerConfig lbConfig;

    public CouchUsersPool() {
        super();
    }

    @Override
    protected LoadBalancerConfig getLoadBalancerConfig() {
        return this.lbConfig;
    }

}
