package de.bornemisza.security.auth;

import javax.security.auth.login.CredentialNotFoundException;
import org.junit.Test;
import static org.junit.Assert.*;

import org.glassfish.jersey.internal.util.Base64;

public class BasicAuthCredentialsTest {

    private final String userName = "Fred";
    private final String password = "secret";
    private final String encodedAuthString = Base64.encodeAsString(userName + ":" + password);
    private final String authHeader = "Basic " + encodedAuthString;

    @Test
    public void decodeAuthString() throws CredentialNotFoundException {
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
        catch (CredentialNotFoundException e) {
            assertTrue(e.getMessage().startsWith("401 AuthHeader broken"));
        }
    }

    @Test
    public void illegalAuthString() throws CredentialNotFoundException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials(encodedAuthString);
            fail(creds.getPassword());
        }
        catch (CredentialNotFoundException e) {
            assertTrue(e.getMessage().startsWith("401 AuthHeader broken"));
        }
    }

    @Test
    public void undecodableCredentials() throws CredentialNotFoundException {
        try {
            BasicAuthCredentials creds = new BasicAuthCredentials("Basic " + Base64.encodeAsString(userName));
        }
        catch (CredentialNotFoundException e) {
            assertTrue(e.getMessage().startsWith("401 AuthHeader unparseable"));
        }
    }

}
