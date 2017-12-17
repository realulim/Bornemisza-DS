package de.bornemisza.sessions.endpoint;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.sessions.boundary.SessionsFacade;

@Stateless
@Path("/")
public class Sessions {

    @Inject
    SessionsFacade facade;

    public Sessions() {
    }

    // Constructor for Unit Tests
    public Sessions(SessionsFacade facade) {
        this.facade = facade;
    }

    @GET
    @Path("new")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNewSession(@HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        Session session;
        try {
            session = facade.createNewSession(authHeader);
        }
        catch (UnauthorizedException ex) {
            return Response.status(Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.toString()).build();
        }
        if (session == null) return Response.status(Status.NOT_FOUND).build();
        else {
            DoubleSubmitToken dsToken = session.getDoubleSubmitToken();
            return Response.ok()
                .header(HttpHeaders.SET_COOKIE, dsToken.getCookie())
                .header(HttpHeaders.CTOKEN, dsToken.getCtoken())
                .build();
        }
    }

    @DELETE
    public Response deleteCookieInBrowser() {
        return Response.ok()
                .header("Cache-Control", "must-revalidate")
                .header("Set-Cookie", "AuthSession=; Version=1; Path=/; HttpOnly; Secure")
                .build();
    }

}
