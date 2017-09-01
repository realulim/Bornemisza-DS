package de.bornemisza.loadbalancer.entity;

import org.junit.Test;
import static org.junit.Assert.*;

public class SrvRecordTest {
    
    @Test
    public void fromStringEquivalentToConstructor() {
        int priority = 1;
        int weight = 0;
        int port = 443;
        String host = "some.host.domain.de.";
        SrvRecord CUT1 = SrvRecord.fromString(priority + " " + weight + " " + port + " " + host);
        SrvRecord CUT2 = new SrvRecord(priority, weight, port, host);
        assertEquals(priority, CUT1.getPriority(), CUT2.getPriority());
        assertEquals(weight, CUT1.getWeight(), CUT2.getWeight());
        assertEquals(port, CUT1.getPort(), CUT2.getPort());
        assertEquals(host, CUT1.getHost(), CUT2.getHost());
    }

}
