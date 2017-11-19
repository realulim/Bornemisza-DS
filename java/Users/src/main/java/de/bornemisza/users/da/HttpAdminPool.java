package de.bornemisza.users.da;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.da.HttpPool;

@Stateless
public class HttpAdminPool extends HttpPool {

    @Resource(name="lbconfig/CouchAdminPool")
    LoadBalancerConfig lbConfig;

    public HttpAdminPool() {
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
