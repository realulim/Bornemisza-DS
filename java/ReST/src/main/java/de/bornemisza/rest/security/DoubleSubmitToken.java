package de.bornemisza.rest.security;

import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.domain.JWTException;

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

    public String getBaseCookie() {
        if (cookie == null) return null;
        else return cookie.contains(";") ? cookie.substring(0, cookie.indexOf(";")) : cookie;
    }

    public String getCtoken() {
        return ctoken;
    }

    public void checkValidity(HashProvider hashProvider) throws UnauthorizedException {
        if (isVoid(cookie) || isVoid(ctoken)) {
            throw new UnauthorizedException(HttpHeaders.COOKIE + " or " + HttpHeaders.CTOKEN + " missing!");
        }
        try {
            String baseCookie = getBaseCookie();
            JWT decodedJasonWebToken = hashProvider.decodeJasonWebToken(ctoken);
            Object cookieClaim = decodedJasonWebToken.claims.get("Cookie");
            if (cookieClaim == null || !cookieClaim.toString().equals(baseCookie)) {
                throw new UnauthorizedException("Hash Mismatch!");
            }
        }
        catch (JWTException ex) {
            throw new UnauthorizedException("JWT invalid: " + ex.toString());
        }
    }

    protected boolean isVoid(String value) {
        if (value == null) return true;
        else if (value.length() == 0) return true;
        else return value.equals("null");
    }

}
