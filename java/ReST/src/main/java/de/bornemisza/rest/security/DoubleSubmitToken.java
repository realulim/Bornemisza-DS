package de.bornemisza.rest.security;

import org.glassfish.jersey.internal.util.Base64;
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
        else if (cookie.startsWith("AuthSession=")) {
            return cookie.contains(";") ? cookie.substring(cookie.indexOf("=") + 1, cookie.indexOf(";")) : cookie.substring(cookie.indexOf("=") + 1);
        }
        else return cookie.contains(";") ? cookie.substring(0, cookie.indexOf(";")) : cookie;
    }

    public String getCtoken() {
        return ctoken;
    }

    /**
     * Check whether a request with this double submit token should be considered valid and authorized.
     * 
     * @param hashProvider the HashProvider to use for verification
     * @return the subject/principal that the ctoken was issued to
     * @throws UnauthorizedException if cookie and ctoken don't match or can't be verified
     */
    public String checkValidity(HashProvider hashProvider) throws UnauthorizedException {
        if (isVoid(cookie) || isVoid(ctoken)) {
            throw new UnauthorizedException(HttpHeaders.COOKIE + " or " + HttpHeaders.CTOKEN + " missing!");
        }
        try {
            String baseCookie = getBaseCookie();
            String decoded = Base64.decodeAsString(baseCookie);
            if (! decoded.contains(":")) throw new UnauthorizedException("Cookie invalid!");
            String cookiePrincipal = decoded.substring(0, decoded.indexOf(":"));
            
            JWT decodedJasonWebToken = hashProvider.decodeJasonWebToken(ctoken);
            if (decodedJasonWebToken.subject == null) throw new UnauthorizedException("JWT invalid!");
            if (! decodedJasonWebToken.subject.equals(cookiePrincipal)) {
                throw new UnauthorizedException("Hash Mismatch!");
            }
            return cookiePrincipal;
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
