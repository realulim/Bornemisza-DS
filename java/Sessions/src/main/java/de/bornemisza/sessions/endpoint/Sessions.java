package de.bornemisza.sessions.endpoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.auth.login.CredentialNotFoundException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.javalite.http.Get;
import org.javalite.http.HttpException;
import org.javalite.http.Post;

import de.bornemisza.couchdb.entity.Session;
import de.bornemisza.rest.BasicAuthCredentials;
import de.bornemisza.sessions.da.HttpSessionsPool;

@Stateless
@Path("/")
public class Sessions {

    @Inject
    HttpSessionsPool sessionsPool;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String CTOKEN_HEADER = "C-Token";

    public Sessions() { }

    // Constructor for Unit Tests
    public Sessions(HttpSessionsPool sessionsPool) {
        this.sessionsPool = sessionsPool;
    }

    @GET
    @Path("new")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNewSession(@HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        BasicAuthCredentials creds;
        try {
            creds = new BasicAuthCredentials(authHeader);
        }
        catch (CredentialNotFoundException ex) {
            throw new RestException(Response.Status.UNAUTHORIZED);
        }
        Post post = sessionsPool.getConnection().post("")
            .param("name", creds.getUserName())
            .param("password", creds.getPassword());
        try {
            int responseCode = post.responseCode();
            if (responseCode != 200) {
                return Response.status(responseCode).entity(post.responseMessage()).build();
            }
        }
        catch (HttpException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.toString()).build();
        }
        Map<String, List<String>> headers = post.headers();
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No Cookie!").build();
        }
        else {
            return Response.ok().header(CTOKEN_HEADER, cookies.get(0)).build();
        }
    }

    @GET
    @Path("active")
    @Produces(MediaType.APPLICATION_JSON)
    public Session getActiveSession(@HeaderParam(CTOKEN_HEADER) String cToken) {
        if (isVoid(cToken)) throw new RestException(
                Response.status(Status.UNAUTHORIZED).entity("No Cookie!").build());
        Get get = sessionsPool.getConnection().get("")
                .header(HttpHeaders.COOKIE, cToken);
        try {
            int responseCode = get.responseCode();
            if (responseCode != 200) {
                throw new RestException(Response.status(responseCode).entity(get.responseMessage()).build());
            }
        }
        catch (HttpException ex) {
            throw new RestException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.toString()).build());
        }
        Session session = new Session();
        JsonNode root;
        try {
            root = mapper.readTree(get.text());
        }
        catch (IOException ioe) {
            throw new RestException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ioe.toString()).build());
        }
        session.setPrincipal(root.path("userCtx").path("name").asText());
        for (JsonNode node : root.path("userCtx").path("roles")) {
            session.addRole(node.asText());
        }
        List<String> cookies = get.headers().get(HttpHeaders.SET_COOKIE);
        if (! (cookies == null || cookies.isEmpty())) session.setCToken(cookies.iterator().next());
        else session.setCToken(cToken);
        return session;
    }

    @DELETE
    public Response deleteCookieInBrowser() {
        return Response.ok()
                .header("Cache-Control", "must-revalidate")
                .header("Set-Cookie", "AuthSession=; Version=1; Path=/; HttpOnly")
                .build();
    }

    private boolean isVoid(String value) {
        if (value == null) return true;
        else if (value.length() == 0) return true;
        else return value.equals("null");
    }

}
