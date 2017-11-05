package de.bornemisza.maintenance;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.couchdb.da.CouchPool;
import de.bornemisza.loadbalancer.LoadBalancerConfig;

@Stateless
public class CouchAdminPool extends CouchPool {

    @Resource(name="lbconfig/CouchAdminPool")
    LoadBalancerConfig lbConfig;

    public CouchAdminPool() {
        super();
    }

    @Override
    protected LoadBalancerConfig getLoadBalancerConfig() {
        return this.lbConfig;
    }

}
