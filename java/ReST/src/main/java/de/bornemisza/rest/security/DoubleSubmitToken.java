package de.bornemisza.rest.security;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.exception.UnauthorizedException;

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

    public void checkValidity(HashProvider hashProvider) throws UnauthorizedException {
        if (isVoid(cookie) || isVoid(ctoken)) {
            throw new UnauthorizedException(HttpHeaders.COOKIE + " or " + HttpHeaders.CTOKEN + " missing!");
        }
        else if (! hashProvider.hmacDigest(cookie).equals(ctoken)) {
            throw new UnauthorizedException("Hash Mismatch!");
        }
        
    }

    protected boolean isVoid(String value) {
        if (value == null) return true;
        else if (value.length() == 0) return true;
        else return value.equals("null");
    }

}
