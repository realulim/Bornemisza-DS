package de.bornemisza.loadbalancer.da;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class HealthChecks {
    
    public HealthChecks() {
    }

    public boolean isHostAvailable(final String hostname, final int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostname, port), 1000);
            return true;
        } 
        catch (IOException ex) {
            return false;
        }
    }

}
