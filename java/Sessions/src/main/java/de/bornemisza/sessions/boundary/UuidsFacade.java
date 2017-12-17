package de.bornemisza.sessions.boundary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.entity.UuidsResult;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.sessions.JAXRSConfiguration;
import de.bornemisza.sessions.da.CouchPool;
import de.bornemisza.sessions.da.DnsResolver;
import de.bornemisza.sessions.da.UuidsService;

@Stateless
public class UuidsFacade {

    @Inject
    UuidsService uuidsService;

    @Inject
    CouchPool couchPool;

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    DnsResolver dnsResolver;

    @Resource(name="lbconfig/CouchUsersAsAdmin")
    LoadBalancerConfig lbConfig;

    DbAdminPasswordBasedHashProvider hashProvider;

    private List<String> allHostnames = new ArrayList<>();
    private final Map<String, String> ipToHostname = new HashMap<>();

    public UuidsFacade() {
    }

    // Constructor for Unit Tests
    public UuidsFacade(UuidsService uuidsService, CouchPool couchPool, HazelcastInstance hazelcast, DnsResolver dnsResolver, LoadBalancerConfig lbConfig) {
        this.uuidsService = uuidsService;
        this.couchPool = couchPool;
        this.hazelcast = hazelcast;
        this.dnsResolver = dnsResolver;
        this.lbConfig = lbConfig;
        init();
    }

    @PostConstruct
    private void init() {
        this.hashProvider = new DbAdminPasswordBasedHashProvider(lbConfig);
        updateColorsForCluster();
        hazelcast.getCluster().addMembershipListener(new MembershipListener() {
            @Override public void memberAdded(MembershipEvent me) { updateColorsForCluster(); }
            @Override public void memberRemoved(MembershipEvent me) { updateColorsForCluster(); }
            @Override public void memberAttributeChanged(MemberAttributeEvent mae) { }
        });
    }

    public Response getUuids(Auth auth, int count) throws UnauthorizedException {
        auth.checkTokenValidity(hashProvider);
        UuidsResult uuidsResult = uuidsService.getUuids(count);
        return Response.ok()
                .header("AppServer", JAXRSConfiguration.MY_COLOR)
                .header("DbServer", getDbServerColor(uuidsResult.getBackendHeader()))
                .entity(uuidsResult).build();
    }

    /**
     * Manage colors within the Hazelcast and CouchDB clusters consistently.
     * Make sure that an AppServer cluster node always selects the same (and unused) color,
     * no matter how many other AppServer nodes are in the Hazelcast cluster.
     * This is achieved by ordering the nodes according to their hostname.
     * The downside is that this will likely not work, when cluster membership changes.
     */
    private void updateColorsForCluster() {
        updateDbServerLookupTables();
        int myIndex = 0;
        String myHostname = hazelcast.getCluster().getLocalMember().getSocketAddress().getHostName();
        List<Member> members = new ArrayList(hazelcast.getCluster().getMembers());
        Collections.sort(members, new MemberComparator());
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

    private void updateDbServerLookupTables() {
        ipToHostname.clear();
        allHostnames = new ArrayList(couchPool.getAllConnections().keySet());
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
