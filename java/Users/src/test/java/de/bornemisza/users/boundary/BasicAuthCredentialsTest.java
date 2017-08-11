package de.bornemisza.users.boundary;

import org.glassfish.jersey.internal.util.Base64;

import org.junit.Test;
import static org.junit.Assert.*;

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
        BasicAuthCredentials CUT = new BasicAuthCredentials(null);
        assertNull(CUT.getUserName());
        assertNull(CUT.getPassword());
    }

    @Test
    public void illegalAuthString() {
        BasicAuthCredentials CUT = new BasicAuthCredentials(encodedAuthString);
        assertNull(CUT.getUserName());
        assertNull(CUT.getPassword());
    }

    @Test
    public void undecodableCredentials() {
        BasicAuthCredentials CUT = new BasicAuthCredentials("Basic " + Base64.encodeAsString(userName));
        assertNull(CUT.getUserName());
        assertNull(CUT.getPassword());
    }

}
