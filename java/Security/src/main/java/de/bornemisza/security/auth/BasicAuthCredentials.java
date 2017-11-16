package de.bornemisza.security.auth;

import javax.security.auth.login.CredentialNotFoundException;

import org.glassfish.jersey.internal.util.Base64;

public class BasicAuthCredentials {

    private final String userName;
    private String password;

    public BasicAuthCredentials(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public BasicAuthCredentials(String authHeader) throws CredentialNotFoundException {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new CredentialNotFoundException("401 AuthHeader broken: " + authHeader);
        }
        else {
            authHeader = authHeader.substring(6);
            String decoded = Base64.decodeAsString(authHeader);
            if (decoded.contains(":")) {
                String[] splitted = decoded.split(":");
                this.userName = splitted[0];
                this.password = splitted[1];
            }
            else {
                throw new CredentialNotFoundException("401 AuthHeader unparseable: " + authHeader);
            }
        }
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

}
