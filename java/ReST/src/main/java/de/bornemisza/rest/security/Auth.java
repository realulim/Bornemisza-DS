package de.bornemisza.rest.security;

import java.io.Serializable;
import java.util.Objects;

import de.bornemisza.rest.exception.UnauthorizedException;
import static de.bornemisza.rest.security.Auth.Scheme.COOKIE_CSRFTOKEN;
import static de.bornemisza.rest.security.Auth.Scheme.USERNAME_PASSWORD;

public class Auth implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.scheme);
        hash = 97 * hash + Objects.hashCode(this.userName);
        hash = 97 * hash + Objects.hashCode(this.cookie);
        hash = 97 * hash + Objects.hashCode(this.csrfToken);
        hash = 97 * hash + Objects.hashCode(this.password);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Auth other = (Auth) obj;
        if (!Objects.equals(this.userName, other.userName)) {
            return false;
        }
        if (!Objects.equals(this.cookie, other.cookie)) {
            return false;
        }
        if (!Objects.equals(this.csrfToken, other.csrfToken)) {
            return false;
        }
        if (!Objects.equals(this.password, other.password)) {
            return false;
        }
        return this.scheme == other.scheme;
    }

}
