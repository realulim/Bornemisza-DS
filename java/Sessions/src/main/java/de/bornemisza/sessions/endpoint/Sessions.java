package de.bornemisza.sessions.endpoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.security.auth.login.CredentialNotFoundException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.Post;

import de.bornemisza.couchdb.da.ConnectionPool;
import de.bornemisza.couchdb.entity.CouchDbConnection;
import de.bornemisza.rest.BasicAuthCredentials;
import de.bornemisza.rest.entity.Session;

@Path("/")
public class Sessions {
    
    @Resource(name="couchdb/Sessions")
    ConnectionPool pool;

    private final ObjectMapper mapper = new ObjectMapper();

    public Sessions() { }

    // Constructor for Unit Tests
    public Sessions(ConnectionPool pool) {
        this.pool = pool;
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
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            CouchDbConnection conn = pool.getConnector().getCouchDbConnection();
            Post post = Http.post(conn.getBaseUrl().toString() + conn.getDatabaseName())
                .param("name", creds.getUserName())
                .param("password", creds.getPassword());
            Map<String, List<String>> headers = post.headers();
            List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
            if (cookies == null || cookies.isEmpty()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No Cookie!").build();
            }
            else {
                return Response.ok().header(HttpHeaders.SET_COOKIE, cookies.get(0)).build();
            }
        }
        catch (RuntimeException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("active")
    @Produces(MediaType.APPLICATION_JSON)
    public Session getActiveSession(@HeaderParam(HttpHeaders.COOKIE) String cookie) throws IOException {
        try {
            CouchDbConnection conn = pool.getConnector().getCouchDbConnection();
            Get get = Http.get(conn.getBaseUrl().toString() + conn.getDatabaseName())
                    .header(HttpHeaders.COOKIE, cookie);
            Session session = new Session();
            JsonNode root = mapper.readTree(get.text());
            session.setPrincipal(root.path("userCtx").path("name").asText());
            for (JsonNode node : root.path("userCtx").path("roles")) {
                session.addRole(node.asText());
            }
            List<String> cookies = get.headers().get(HttpHeaders.SET_COOKIE);
            if (! (cookies == null || cookies.isEmpty())) session.setCookie(cookies.iterator().next());
            else session.setCookie(cookie);
            return session;
        }
        catch (RuntimeException ex) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
    }

}
