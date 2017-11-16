package de.bornemisza.security;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;

public class HashProviderTest {

    private HashProvider CUT;
    private String testMessage;

    public HashProviderTest() {
    }

    @Before
    public void setUp() {
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn("My Secret Password".toCharArray());
        CUT = new HashProvider(lbConfig);
        testMessage = "My Test " + System.currentTimeMillis();
    }
    
    @Test
    public void digestIsPredictable() throws Exception {
        String hmac = CUT.hmacDigest(testMessage);
        for (int i = 0; i < 1000; i++) {
            assertEquals(hmac, CUT.hmacDigest(testMessage));
        }
    }
    
}
