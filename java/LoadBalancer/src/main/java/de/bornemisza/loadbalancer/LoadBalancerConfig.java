package de.bornemisza.loadbalancer;

public class LoadBalancerConfig {

    private final String serviceName;
    private final String instanceName;
    private final String userName;
    private final char[] password;

    /**
     * @param serviceName the name under which the service can be located, e. g. in a SRV record (not null)
     * @param instanceName the name of the instance of the service that is to be used, e. g. a database name (not null)
     * @param userName null
     * @param password null
     */
    public LoadBalancerConfig(String serviceName, String instanceName, String userName, char[] password) {
        this.serviceName = serviceName;
        this.instanceName = instanceName;
        this.userName = userName;
        this.password = password;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getUserName() {
        return userName;
    }

    public char[] getPassword() {
        return password;
    }

}
