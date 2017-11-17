package de.bornemisza.security;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;

public class HashProviderTest {

    private HashProvider CUT;
    private String testMessage;
    private LoadBalancerConfig loadBalancerConfig;

    public HashProviderTest() {
    }

    @Before
    public void setUp() {
        loadBalancerConfig = mock(LoadBalancerConfig.class);
        when(loadBalancerConfig.getPassword()).thenReturn("My Secret Password".toCharArray());
        CUT = new HashProviderImpl();
        testMessage = "My Test " + System.currentTimeMillis();
    }
    
    @Test
    public void digestIsPredictable() throws Exception {
        String hmac = CUT.hmacDigest(testMessage);
        for (int i = 0; i < 1000; i++) {
            assertEquals(hmac, CUT.hmacDigest(testMessage));
        }
    }

    public final class HashProviderImpl extends HashProvider {

        public HashProviderImpl() {
            super();
            init();
        }

        @Override
        protected LoadBalancerConfig getLoadBalancerConfig() {
            return loadBalancerConfig;
        }
        
    }

}
