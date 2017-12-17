package de.bornemisza.rest.security;

import de.bornemisza.rest.exception.UnauthorizedException;
import static de.bornemisza.rest.security.Auth.Scheme.COOKIE_CSRFTOKEN;
import static de.bornemisza.rest.security.Auth.Scheme.USERNAME_PASSWORD;

public class Auth {

    private final Scheme scheme;
    private final String userName, cookie, csrfToken;
    private String password;

    public enum Scheme { USERNAME_PASSWORD, COOKIE_CSRFTOKEN }

    public Auth(BasicAuthCredentials creds) {
        this.scheme = USERNAME_PASSWORD;
        this.userName = creds.getUserName();
        this.password = creds.getPassword();
        this.cookie = null;
        this.csrfToken = null;
    }

    public Auth(DoubleSubmitToken dsToken) {
        this.scheme = COOKIE_CSRFTOKEN;
        this.userName = null;
        this.password = null;
        this.cookie = dsToken.getCookie();
        this.csrfToken = dsToken.getCtoken();
    }

    public Scheme getScheme() {
        return scheme;
    }

    /**
     * @return the username to be used for authentication or null, if no username is part of the authentication scheme
     */
    public String getUsername() {
        return this.userName;
    }

    /**
     * @return the password to be used for authentication or null, if no password is part of the authentication scheme
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * @return the cookie to be used for authentication or null, if no cookie is part of the authentication scheme
     */
    public String getCookie() {
        return this.cookie;
    }

    /**
     * @return the csrf token to be used for authentication or null, if no csrf token is part of the authentication scheme
     */
    public String getCsrfToken() {
        return this.csrfToken;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public String checkTokenValidity(HashProvider hashProvider) throws UnauthorizedException {
        return new DoubleSubmitToken(cookie, csrfToken).checkValidity(hashProvider);
    }

}
