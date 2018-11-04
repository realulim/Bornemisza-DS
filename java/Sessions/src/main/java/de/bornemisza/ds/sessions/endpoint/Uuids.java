package de.bornemisza.ds.sessions.endpoint;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.result.KeyValueViewResult;
import de.bornemisza.rest.entity.result.UuidsResult;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.ds.sessions.boundary.UuidsFacade;

@Stateless
@Path("/uuid")
public class Uuids {

    @Inject
    UuidsFacade facade;

    public Uuids() { }

    // Constructor for Unit Tests
    public Uuids(UuidsFacade facade) {
        this.facade = facade;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUuids(@HeaderParam(HttpHeaders.COOKIE) String cookie,
                             @HeaderParam(HttpHeaders.CTOKEN) String ctoken,
                             @DefaultValue("1")@QueryParam("count") int count) {
        if (count < 1) return Response.status(Status.BAD_REQUEST).build();
        try {
            Auth auth = new Auth(new DoubleSubmitToken(cookie, ctoken));
            UuidsResult result = facade.getAndSaveUuids(auth, count);
            return result.toResponse();
        }
        catch (UnauthorizedException ex) {
            return Response.status(Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.toString()).build();
        }
    }

    @GET
    @Path("colors/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadColors(@HeaderParam(HttpHeaders.COOKIE) String cookie,
                               @HeaderParam(HttpHeaders.CTOKEN) String ctoken) {
        try {
            Auth auth = new Auth(new DoubleSubmitToken(cookie, ctoken));
            KeyValueViewResult result = facade.loadColors(auth);
            return result.toResponse();
        }
        catch (UnauthorizedException ex) {
            return Response.status(Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

}
