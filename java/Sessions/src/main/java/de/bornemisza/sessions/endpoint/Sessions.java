package de.bornemisza.sessions.endpoint;

import javax.inject.Inject;
import javax.ws.rs.Path;

import de.bornemisza.sessions.boundary.SessionsFacade;

@Path("/")
public class Sessions {
    
    @Inject
    SessionsFacade facade;

    public Sessions() { }

    // Constructor for Unit Tests
    public Sessions(SessionsFacade facade) {
        this.facade = facade;
    }

}
