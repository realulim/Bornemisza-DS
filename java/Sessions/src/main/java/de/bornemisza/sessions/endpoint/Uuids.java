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

import de.bornemisza.rest.Http;
import de.bornemisza.sessions.JAXRSConfiguration;
import de.bornemisza.sessions.da.DnsResolver;
import de.bornemisza.sessions.da.HttpBasePool;

@Stateless
@Path("/uuid")
public class Uuids {

    @Inject
    HttpBasePool basePool;

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    DnsResolver dnsResolver;

    private List<String> allHostnames = new ArrayList<>();
    private final Map<String, String> ipToHostname = new HashMap<>();

    private static final String CTOKEN_HEADER = "C-Token";

    public Uuids() { }

    // Constructor for Unit Tests
    public Uuids(HttpBasePool basePool, HazelcastInstance hazelcast, DnsResolver dnsResolver) {
        this.basePool = basePool;
        this.hazelcast = hazelcast;
        this.dnsResolver = dnsResolver;
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
        List<Member> members = new ArrayList(hazelcast.getCluster().getMembers());
        Collections.sort(members, new MemberComparator());
        String myHostname = hazelcast.getCluster().getLocalMember().getSocketAddress().getHostName();
        int myIndex = 0;
        allHostnames = new ArrayList(basePool.getAllHostnames());
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
    public Response getUuids(@HeaderParam(CTOKEN_HEADER) String cToken,
                             @DefaultValue("1")@QueryParam("count") int count) {
        if (isVoid(cToken)) throw new RestException(
                Response.status(Status.UNAUTHORIZED).entity("No Cookie!").build());
        Http httpBase = basePool.getConnection();
        Get get = httpBase.get("_uuids?count=" + count)
                .header(HttpHeaders.COOKIE, cToken);
        if (get.responseCode() != 200) {
            throw new RestException(
                    Response.status(get.responseCode()).entity(get.responseMessage()).build());
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

    private String getDbServerColor(String ipAddressHeader) {
        String hostname = ipToHostname.get(ipAddressHeader);
        if (hostname == null) return JAXRSConfiguration.DEFAULT_COLOR;
        int index = allHostnames.indexOf(hostname);
        if (index == -1) return JAXRSConfiguration.DEFAULT_COLOR;
        else return JAXRSConfiguration.COLORS.get(index);
    }

    private static class MemberComparator implements Comparator<Member> {
        @Override public int compare(Member m1, Member m2) {
            return m1.getSocketAddress().getHostName().compareTo(m2.getSocketAddress().getHostName());
        }
    }

}
