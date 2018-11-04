package de.bornemisza.ds.rest.security;

import de.bornemisza.ds.rest.security.HashProvider;
import org.junit.Before;
import org.junit.Test;
import org.primeframework.jwt.domain.JWT;
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

    @Test
    public void encodeDecodeJasonWebToken() {
        String userName = "Drombo van Cleefy";
        String encodedJWT = CUT.encodeJasonWebToken(userName);
        JWT jwt = CUT.decodeJasonWebToken(encodedJWT);
        assertEquals(userName, jwt.subject);
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
