package de.bornemisza.maintenance;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.da.HttpPool;

@Stateless
public class HttpBasePool extends HttpPool {

    @Resource(name = "lbconfig/HttpBase")
    LoadBalancerConfig lbConfig;

    public HttpBasePool() {
        super();
    }

    @Override
    protected LoadBalancerConfig getLoadBalancerConfig() {
        return this.lbConfig;
    }

}
