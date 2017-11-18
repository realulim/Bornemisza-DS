package de.bornemisza.rest.security;

public class DoubleSubmitToken {

    private final String cookie;
    private final String ctoken;

    public DoubleSubmitToken(String cookie, String ctoken) {
        this.cookie = cookie;
        this.ctoken = ctoken;
    }

    public String getCookie() {
        return cookie;
    }

    public String getCtoken() {
        return ctoken;
    }

}
