package de.bornemisza.sessions.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import org.javalite.http.Get;
import org.javalite.http.HttpException;

import de.bornemisza.rest.Http;
import de.bornemisza.sessions.JAXRSConfiguration;
import de.bornemisza.sessions.da.DnsResolver;
import de.bornemisza.sessions.da.HttpBasePool;
import de.bornemisza.sessions.security.HashProvider;

@Stateless
@Path("/uuid")
public class Uuids {

    @Inject
    HttpBasePool basePool;

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    DnsResolver dnsResolver;

    @Inject
    HashProvider hashProvider;

    private List<String> allHostnames = new ArrayList<>();
    private final Map<String, String> ipToHostname = new HashMap<>();

    public Uuids() { }

    // Constructor for Unit Tests
    public Uuids(HttpBasePool basePool, HazelcastInstance hazelcast, DnsResolver dnsResolver, HashProvider hashProvider) {
        this.basePool = basePool;
        this.hazelcast = hazelcast;
        this.dnsResolver = dnsResolver;
        this.hashProvider = hashProvider;
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
     * This is achieved by ordering the nodes according to their IP addresses.
     */
    private void updateColorsForCluster() {
        ipToHostname.clear();
        List<Member> members = new ArrayList(hazelcast.getCluster().getMembers());
        Collections.sort(members, new MemberComparator());
        String myHostname = hazelcast.getCluster().getLocalMember().getSocketAddress().getHostName();
        int myIndex = 0;
        allHostnames = new ArrayList(basePool.getAllConnections().keySet());
        Collections.sort(allHostnames);
        for (String hostname : allHostnames) {
            String ip = dnsResolver.getHostAddress("internal." + hostname);
            if (ip != null) {
                ipToHostname.put(ip, hostname);
            }
            else {
                // should never happen, but if it does, we'll live with the default color
                Logger.getAnonymousLogger().severe("Cannot resolve internal." + hostname);
            }
        }
        for (Member member : members) {
            if (member.getSocketAddress().getHostName().equals(myHostname)) {
                if (myIndex >= JAXRSConfiguration.COLORS.size()) {
                    // use default color for any overflow
                    JAXRSConfiguration.MY_COLOR = JAXRSConfiguration.DEFAULT_COLOR;
                }
                else {
                    // use one of the predefined colors
                    JAXRSConfiguration.MY_COLOR = JAXRSConfiguration.COLORS.get(myIndex);
                }
            }
            Logger.getAnonymousLogger().info("Assigning " + JAXRSConfiguration.MY_COLOR + " to " + myHostname);
            myIndex++;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUuids(@HeaderParam(HttpHeaders.COOKIE) String cookie,
                             @HeaderParam(Sessions.CTOKEN) String ctoken,
                             @DefaultValue("1")@QueryParam("count") int count) {
        if (isVoid(cookie) || isVoid(ctoken)) throw new RestException(
                Response.status(Status.UNAUTHORIZED).entity("Cookie or " + Sessions.CTOKEN + " missing!").build());
        else if (! hashMatches(cookie, ctoken)) throw new RestException(
                Response.status(Status.UNAUTHORIZED).entity("Hash Mismatch!").build());
        Http httpBase = basePool.getConnection();
        Get get = httpBase.get("_uuids?count=" + count)
                .header(HttpHeaders.COOKIE, cookie);
        try {
            int responseCode = get.responseCode();
            if (responseCode != 200) {
                return Response.status(responseCode).entity(get.responseMessage()).build();
            }
        }
        catch (HttpException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.toString()).build();
        }
        String header = "127.0.0.1";
        List<String> backendHeaders = get.headers().get("X-Backend");
        if (backendHeaders != null) header = backendHeaders.get(0);
        return Response.ok()
                .header("AppServer", JAXRSConfiguration.MY_COLOR)
                .header("DbServer", getDbServerColor(header))
                .entity(get.text()).build();
    }

    private boolean isVoid(String value) {
        if (value == null) return true;
        else if (value.length() == 0) return true;
        else return value.equals("null");
    }

    private boolean hashMatches(String cookie, String hmac) {
        return hashProvider.hmacDigest(cookie).equals(hmac);
    }

    private String getDbServerColor(String ipAddressHeader) {
        String hostname = ipToHostname.get(ipAddressHeader);
        if (hostname == null) {
            updateColorsForCluster();
            Logger.getAnonymousLogger().warning("No Hostname found for " + ipAddressHeader);
            return JAXRSConfiguration.DEFAULT_COLOR;
        }
        int index = allHostnames.indexOf(hostname);
        return JAXRSConfiguration.COLORS.get(index);
    }

    private static class MemberComparator implements Comparator<Member> {
        @Override public int compare(Member m1, Member m2) {
            return m1.getSocketAddress().getHostName().compareTo(m2.getSocketAddress().getHostName());
        }
    }

}
