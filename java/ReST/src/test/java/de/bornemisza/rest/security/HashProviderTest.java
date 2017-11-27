package de.bornemisza.rest.security;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class HashProviderTest {

    private HashProvider CUT;
    private String testMessage;

    public HashProviderTest() {
    }

    @Before
    public void setUp() {
        CUT = new HashProviderImpl();
        testMessage = "My Test " + System.currentTimeMillis();
    }

    @Test
    public void digestIsPredictable() throws Exception {
        String hmac = CUT.hmacDigest(testMessage);
        for (int i = 0; i < 100; i++) {
            assertEquals(hmac, CUT.hmacDigest(testMessage));
        }
        CUT = new HashProviderImpl();
        for (int i = 0; i < 100; i++) {
            assertEquals(hmac, CUT.hmacDigest(testMessage));
        }
    }

    public final class HashProviderImpl extends HashProvider {

        public HashProviderImpl() {
            super();
            init();
        }

        @Override
        protected char[] getServerSecret() {
            return "My Secret Password".toCharArray();
        }

    }

}
