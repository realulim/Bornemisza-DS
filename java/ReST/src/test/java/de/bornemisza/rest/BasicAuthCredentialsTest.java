package de.bornemisza.rest;

import org.junit.Test;
import static org.junit.Assert.*;

import org.glassfish.jersey.internal.util.Base64;

public class BasicAuthCredentialsTest {

    private final String userName = "Fred";
    private final String password = "secret";
    private final String encodedAuthString = Base64.encodeAsString(userName + ":" + password);
    private final String authHeader = "Basic " + encodedAuthString;

    @Test
    public void decodeAuthString() {
        BasicAuthCredentials CUT = new BasicAuthCredentials(authHeader);
        assertEquals(userName, CUT.getUserName());
        assertEquals(password, CUT.getPassword());
    }

    @Test
    public void nullAuthString() {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(null);
            fail(creds.getPassword());
        }
        catch (UnauthorizedException e) {
            assertTrue(e.getMessage().startsWith("AuthHeader broken"));
        }
    }

    @Test
    public void illegalAuthString() {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(encodedAuthString);
            fail(creds.getPassword());
        }
        catch (UnauthorizedException e) {
            assertTrue(e.getMessage().startsWith("AuthHeader broken"));
        }
    }

    @Test
    public void undecodableCredentials() {
        BasicAuthCredentials CUT = new BasicAuthCredentials("Basic " + Base64.encodeAsString(userName));
        assertNull(CUT.getUserName());
        assertNull(CUT.getPassword());
    }

}
