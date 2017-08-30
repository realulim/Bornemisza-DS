package de.bornemisza.sessions.boundary;

import javax.ejb.Stateless;
import javax.inject.Inject;

import de.bornemisza.sessions.da.SessionsService;

@Stateless
public class SessionsFacade {
    
    @Inject
    SessionsService sessionsService;

    public SessionsFacade() { }

    // Constructor for Unit Tests
    public SessionsFacade(SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

}
