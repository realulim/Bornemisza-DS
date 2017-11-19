package de.bornemisza.sessions.da;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.da.HttpPool;

@Stateless
public class CouchSessionsPool extends HttpPool {

    @Resource(name = "lbconfig/CouchSessions")
    LoadBalancerConfig lbConfig;

    public CouchSessionsPool() {
        super();
    }

    @Override
    protected LoadBalancerConfig getLoadBalancerConfig() {
        return this.lbConfig;
    }

}
