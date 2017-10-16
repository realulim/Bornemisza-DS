package de.bornemisza.sessions.da;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import javax.ejb.Stateless;

@Stateless
public class DnsResolver {

    public DnsResolver() {
    }

    public String getHostAddress(String hostname) {
        try {
            return InetAddress.getByName(hostname).getHostAddress();
        }
        catch (UnknownHostException ex) {
            Logger.getAnonymousLogger().warning("DNS Problem: " + ex.toString());
            return null;
        }
    }

}
