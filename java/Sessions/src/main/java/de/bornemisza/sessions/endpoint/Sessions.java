package de.bornemisza.sessions.endpoint;

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

import org.javalite.http.HttpException;
import org.javalite.http.Post;

import de.bornemisza.rest.BasicAuthCredentials;
import de.bornemisza.security.HashProvider;
import de.bornemisza.sessions.da.HttpSessionsPool;

@Stateless
@Path("/")
public class Sessions {

    @Inject
    HttpSessionsPool sessionsPool;

    @Inject
    HashProvider hashProvider;

    public static final String CTOKEN = "C-Token";

    public Sessions() {
    }

    // Constructor for Unit Tests
    public Sessions(HttpSessionsPool sessionsPool, HashProvider hashProvider) {
        this.sessionsPool = sessionsPool;
        this.hashProvider = hashProvider;
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
            String cookie = cookies.get(0);
            String hmac = hashProvider.hmacDigest(cookie.substring(0, cookie.indexOf(";")));
            return Response.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie)
                    .header(CTOKEN, hmac)
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
