package de.bornemisza.sessions.boundary;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.auth.login.CredentialNotFoundException;

import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.sessions.da.SessionsService;

@Stateless
public class SessionsFacade {
    
    @Inject
    SessionsService sessionsService;

    public SessionsFacade() {
    }

    // Constructor for Unit Tests
    public SessionsFacade(SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    public Session createNewSession(String authHeader) {
        BasicAuthCredentials creds;
        try {
            creds = new BasicAuthCredentials(authHeader);
            return sessionsService.createSession(creds);
        }
        catch (CredentialNotFoundException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

}
