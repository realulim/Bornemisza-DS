package de.bornemisza.users.da;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.couchdb.da.CouchPool;
import de.bornemisza.loadbalancer.LoadBalancerConfig;

@Stateless
public class CouchUsersPool extends CouchPool {

    @Resource(name="lbconfig/CouchUsersPool")
    LoadBalancerConfig lbConfig;

    public CouchUsersPool() {
        super();
    }

    @Override
    protected LoadBalancerConfig getLoadBalancerConfig() {
        return this.lbConfig;
    }

}
