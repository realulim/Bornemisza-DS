package de.bornemisza.users.boundary;

import org.glassfish.jersey.internal.util.Base64;

public class BasicAuthCredentials {

    private final String userName;
    private final String password;

    public BasicAuthCredentials(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            this.userName = null;
            this.password = null;
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
                this.userName = null;
                this.password = null;
            }
        }
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

}
