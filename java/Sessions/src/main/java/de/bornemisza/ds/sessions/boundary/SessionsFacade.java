package de.bornemisza.ds.sessions.boundary;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.auth.login.CredentialNotFoundException;

import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.ds.sessions.da.SessionsService;

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

    public Session createNewSession(String authHeader) throws BusinessException, TechnicalException, UnauthorizedException {
        BasicAuthCredentials creds;
        try {
            creds = new BasicAuthCredentials(authHeader);
            return sessionsService.createSession(new Auth(creds));
        }
        catch (CredentialNotFoundException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

}
