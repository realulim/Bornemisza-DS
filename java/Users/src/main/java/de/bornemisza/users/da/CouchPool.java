package de.bornemisza.users.da;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.da.HttpPool;

@Stateless
public class CouchPool extends HttpPool {

    @Resource(name = "lbconfig/Couch")
    LoadBalancerConfig lbConfig;

    public CouchPool() {
        super();
    }

    @Override
    protected LoadBalancerConfig getLoadBalancerConfig() {
        return this.lbConfig;
    }

}
