package de.bornemisza.security.auth;

import javax.inject.Inject;

import de.bornemisza.security.HashProvider;

public class DoubleSubmitToken {

    @Inject
    HashProvider hashProvider;

    private final String cookie;
    private final String ctoken;

    public DoubleSubmitToken(String cookie, String ctoken) {
        this.cookie = cookie;
        this.ctoken = ctoken;
    }

    // Constructor for Unit Tests
    public DoubleSubmitToken(HashProvider hashProvider, String cookie, String ctoken) {
        this.hashProvider = hashProvider;
        this.cookie = cookie;
        this.ctoken = ctoken;
    }

    public String getCookie() {
        return cookie;
    }

    public String getCtoken() {
        return ctoken;
    }

    public boolean hashMatches() {
        return hashProvider.hmacDigest(cookie).equals(ctoken);
    }

}
