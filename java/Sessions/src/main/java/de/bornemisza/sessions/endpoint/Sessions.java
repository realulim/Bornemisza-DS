package de.bornemisza.sessions.endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.security.auth.login.CredentialNotFoundException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.javalite.http.Get;
import org.javalite.http.Post;

import de.bornemisza.couchdb.entity.Session;
import de.bornemisza.rest.BasicAuthCredentials;
import de.bornemisza.rest.Http;
import de.bornemisza.rest.da.HttpPool;
import de.bornemisza.sessions.JAXRSConfiguration;
import java.util.logging.Logger;

@Path("/")
public class Sessions {

    @Resource(name="http/Sessions")
    HttpPool sessionsPool;

    @Resource(name="http/Base")
    HttpPool basePool;

    @Inject
    HazelcastInstance hazelcast;

    private final ObjectMapper mapper = new ObjectMapper();
    private List<String> allHostnames = new ArrayList<>();

    private static final String CTOKEN_HEADER = "C-Token";

    public Sessions() {
Logger.getAnonymousLogger().info("Constructor Sessions");
    }

    // Constructor for Unit Tests
    public Sessions(HttpPool sessionsPool, HttpPool basePool, HazelcastInstance hazelcast) {
        this.sessionsPool = sessionsPool;
        this.basePool = basePool;
        this.hazelcast = hazelcast;
        init();
    }

    @PostConstruct
    private void init() {
        updateColorsForCluster();
        hazelcast.getCluster().addMembershipListener(new MembershipListener() {
            @Override public void memberAdded(MembershipEvent me) { updateColorsForCluster(); }
            @Override public void memberRemoved(MembershipEvent me) { updateColorsForCluster(); }
            @Override public void memberAttributeChanged(MemberAttributeEvent mae) { }
        });
    }

    /**
     * Manage colors within the Hazelcast and CouchDB clusters consistently.
     * Make sure that an AppServer cluster node always selects the same (and unused) color,
     * no matter how many other AppServer nodes are in the Hazelcast cluster.
     * This is achieved by ordering the nodes according to their Hazelcast UUIDs.
     */
    private void updateColorsForCluster() {
        List<Member> members = new ArrayList(hazelcast.getCluster().getMembers());
        Collections.sort(members, new MemberComparator());
        String myUuid = hazelcast.getCluster().getLocalMember().getUuid();
        int myIndex = 0;
        allHostnames = new ArrayList(basePool.getAllHostnames());
        Collections.sort(allHostnames);
        for (Member member : members) {
            if (member.getUuid().equals(myUuid)) {
                if (myIndex >= JAXRSConfiguration.COLORS.size()) {
                    // use default color for any overflow nodes
                    JAXRSConfiguration.MY_COLOR = JAXRSConfiguration.DEFAULT_COLOR;
                }
                else {
                    // use one of the predefined colors
                    JAXRSConfiguration.MY_COLOR = JAXRSConfiguration.COLORS.get(myIndex);
                }
            }
            myIndex++;
        }
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
        Post post = sessionsPool.getConnection().post("")
            .param("name", creds.getUserName())
            .param("password", creds.getPassword());
        if (post.responseCode() != 200) {
            return Response.status(post.responseCode()).entity(post.responseMessage()).build();
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
        if (isVoid(cToken)) throw new WebApplicationException(
                Response.status(Status.UNAUTHORIZED).entity("No Cookie!").build());
        Get get = sessionsPool.getConnection().get("")
                .header(HttpHeaders.COOKIE, cToken);
        if (get.responseCode() != 200) {
            throw new WebApplicationException(
                    Response.status(get.responseCode()).entity(get.responseMessage()).build());
        }
        Session session = new Session();
        JsonNode root;
        try {
            root = mapper.readTree(get.text());
        }
        catch (IOException ioe) {
            throw new WebApplicationException(
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

    @GET
    @Path("uuid")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUuids(@HeaderParam(CTOKEN_HEADER) String cToken,
                             @DefaultValue("1")@QueryParam("count") int count) {
        if (isVoid(cToken)) throw new WebApplicationException(
                Response.status(Status.UNAUTHORIZED).entity("No Cookie!").build());
Logger.getAnonymousLogger().info("Getting Connection");
        Http httpBase = basePool.getConnection();
Logger.getAnonymousLogger().info("Got Connection");
        Get get = httpBase.get("_uuids?count=" + count)
                .header(HttpHeaders.COOKIE, cToken);
        if (get.responseCode() != 200) {
            throw new WebApplicationException(
                    Response.status(get.responseCode()).entity(get.responseMessage()).build());
        }
        return Response.ok()
                .header("AppServer", JAXRSConfiguration.MY_COLOR)
                .header("DbServer", getDbServerColor(httpBase))
                .entity(get.text()).build();
    }

    private boolean isVoid(String value) {
        if (value == null) return true;
        else if (value.length() == 0) return true;
        else return value.equals("null");
    }

    private String getDbServerColor(Http http) {
        String urlWithoutScheme = http.getBaseUrl().split("://")[1];
        String hostname = urlWithoutScheme.substring(0, urlWithoutScheme.indexOf("/"));
        int index = allHostnames.indexOf(hostname);
        if (index == -1) {
            throw new WebApplicationException(
                    Response.serverError()
                    .entity("Hostname " + hostname + " not found in " + String.join(", ", allHostnames))
                    .build());
        }
        return JAXRSConfiguration.COLORS.get(index);
    }

    private static class MemberComparator implements Comparator<Member> {
        @Override public int compare(Member m1, Member m2) {
            return m1.getUuid().compareTo(m2.getUuid());
        }
    }

}
