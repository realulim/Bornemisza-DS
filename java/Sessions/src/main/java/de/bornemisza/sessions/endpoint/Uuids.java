package de.bornemisza.sessions.endpoint;

import java.util.List;
import java.util.Map;

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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.UuidsResult;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.sessions.boundary.UuidsFacade;

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
        try {
            Auth auth = new Auth(new DoubleSubmitToken(cookie, ctoken));
            UuidsResult result = facade.getUuids(auth, count);
            ResponseBuilder response = Response.status(result.getStatus());
            response.entity(result);
            for (Map.Entry<String, List<String>> entry : result.getHeaders().entrySet()) {
                for (String value : entry.getValue()) {
                    response.header(entry.getKey(), value);
                }
            }
            return response.build();
        }
        catch (UnauthorizedException ex) {
            return Response.status(Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

}
