package de.bornemisza.sessions.da;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.da.HttpPool;

@Stateless
public class HttpSessionsPool extends HttpPool {

    @Resource(name = "lbconfig/HttpSessions")
    LoadBalancerConfig lbConfig;

    public HttpSessionsPool() {
        super();
    }

    @Override
    protected LoadBalancerConfig getLoadBalancerConfig() {
        return this.lbConfig;
    }

}
