package de.bornemisza.sessions.endpoint;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
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

import org.codehaus.jackson.map.ObjectMapper;
import org.javalite.http.Get;

import de.bornemisza.rest.Http;
import de.bornemisza.rest.da.HttpPool;
import de.bornemisza.sessions.JAXRSConfiguration;

@Path("/uuid")
public class Uuids {

    @Resource(name="http/Base")
    HttpPool basePool;

    @Inject
    HazelcastInstance hazelcast;

    private final ObjectMapper mapper = new ObjectMapper();
    private List<String> allHostnames = new ArrayList<>();
    private Map<String, String> ipToHostname = new HashMap<>();

    private static final String CTOKEN_HEADER = "C-Token";

    public Uuids() { }

    // Constructor for Unit Tests
    public Uuids(HttpPool basePool, HazelcastInstance hazelcast) {
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
        for (String hostname : allHostnames) {
            InetAddress addr;
            try {
                addr = InetAddress.getByName("internal." + hostname);
                ipToHostname.put(addr.getHostAddress(), hostname);
            }
            catch (UnknownHostException ex) {
                // should never happen, but if it does, we'll live with the default color
                Logger.getAnonymousLogger().severe(ex.toString());
            }
        }
        for (Member member : members) {
            if (member.getUuid().equals(myUuid)) {
                if (myIndex >= JAXRSConfiguration.COLORS.size()) {
                    // use default color for any overflow
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUuids(@HeaderParam(CTOKEN_HEADER) String cToken,
                             @DefaultValue("1")@QueryParam("count") int count) {
        if (isVoid(cToken)) throw new WebApplicationException(
                Response.status(Status.UNAUTHORIZED).entity("No Cookie!").build());
        Http httpBase = basePool.getConnection();
        Get get = httpBase.get("_uuids?count=" + count)
                .header(HttpHeaders.COOKIE, cToken);
        if (get.responseCode() != 200) {
            throw new WebApplicationException(
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
            return m1.getUuid().compareTo(m2.getUuid());
        }
    }

}
