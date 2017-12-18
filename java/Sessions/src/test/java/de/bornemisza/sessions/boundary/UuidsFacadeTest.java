package de.bornemisza.sessions.boundary;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.UuidsResult;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.rest.security.HashProvider;
import de.bornemisza.sessions.JAXRSConfiguration;
import de.bornemisza.sessions.da.CouchPool;
import de.bornemisza.sessions.da.DnsResolver;
import de.bornemisza.sessions.da.UuidsService;

public class UuidsFacadeTest {

    private UuidsFacade CUT;
    private UuidsService uuidsService;
    private HazelcastInstance hazelcast;
    private CouchPool pool;
    private DnsResolver dnsResolver;
    private final List<String> ipAddresses = new ArrayList<>();
    private final String password = "My secret Password";
    private String cookie, jwt;
    private Auth auth;

    @Before
    public void setUp() {
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn(password.toCharArray());
        HashProvider hashProvider = new DbAdminPasswordBasedHashProvider(lbConfig);
        cookie = "AuthSession=RmF6aWwgT25ndWRhcjo1QTM2Nzc5Rg==";
        jwt = hashProvider.encodeJasonWebToken("Fazil Ongudar");
        DoubleSubmitToken dsToken = new DoubleSubmitToken(cookie, jwt);
        auth = new Auth(dsToken);

        pool = mock(CouchPool.class);

        hazelcast = mock(HazelcastInstance.class);
        Set<Member> members = createMembers(5);
        Cluster cluster = createCluster(members, "db3.domain.de"); // third AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);

        this.dnsResolver = mock(DnsResolver.class);
        when(dnsResolver.getHostAddress(anyString())).thenAnswer(new IpAddressAnswer());

        lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn(password.toCharArray());

        uuidsService = mock(UuidsService.class);
        CUT = new UuidsFacade(uuidsService, pool, hazelcast, dnsResolver, lbConfig);
    }

    class IpAddressAnswer implements Answer {
        private int i = 0;
        @Override public String answer(InvocationOnMock iom) throws Throwable {
            if (i >= ipAddresses.size()) return null;
            String ipAddress = ipAddresses.get(i);
            i++;
            return ipAddress;
        }
    }

    private Set<Member> createMembers(int count) {
        Map<String, HttpConnection> allConnections = new HashMap<>();
        Set<Member> members = new HashSet<>();
        for (int i = 1; i <= count; i++) {
            String hostname = "db" + i + ".domain.de";
            allConnections.put(hostname, mock(HttpConnection.class));
            ipAddresses.add("192.168.0." + i);

            Member member = mock(Member.class);
            InetSocketAddress address = mock(InetSocketAddress.class);
            when(address.getHostName()).thenReturn(hostname);
            when(member.getSocketAddress()).thenReturn(address);
            members.add(member);
        }
        when(pool.getAllConnections()).thenReturn(allConnections);
        return members;
    }

    private Cluster createCluster(Set<Member> members, String myHostname) {
        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(members);
        Member myself = mock(Member.class);
        when(cluster.getLocalMember()).thenReturn(myself);
        InetSocketAddress address = mock(InetSocketAddress.class);
        when(address.getHostName()).thenReturn(myHostname);
        when(myself.getSocketAddress()).thenReturn(address);
        return cluster;
    }

    @Test
    public void getUuids_noCookie() {
        try {
            CUT.getUuids(new Auth(new DoubleSubmitToken("", "")), 3);
            fail();
        }
        catch (UnauthorizedException e) {
            assertEquals("Cookie or C-Token missing!", e.getMessage());
        }
    }

    @Test
    public void getUuids_hashMismatch() {
        try {
            CUT.getUuids(new Auth(new DoubleSubmitToken(cookie, "this-is-not-a-jwt")), 1);
            fail();
        }
        catch (UnauthorizedException e) {
            assertTrue(e.getMessage().startsWith("JWT invalid"));
        }
    }

    @Test
    public void getUuids_unresolvableHostname() {
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn(password.toCharArray());
        UuidsResult uuidsResult = new UuidsResult();
        uuidsResult.addHeader(HttpHeaders.BACKEND, "192.168.0.5");
        uuidsResult.setUuids(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737009adb" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(uuidsResult);
        CUT = new UuidsFacade(uuidsService, pool, hazelcast, dnsResolver, lbConfig);

        Response response = CUT.getUuids(auth, 1);
        assertEquals(200, response.getStatus());
        assertEquals(uuidsResult, response.getEntity());
        assertEquals("Black", response.getHeaderString("DbServer")); // second color
    }

    @Test
    public void getUuids() {
        UuidsResult uuidsResult = new UuidsResult();
        uuidsResult.addHeader(HttpHeaders.BACKEND, "192.168.0." + 2); // second color
        uuidsResult.setUuids(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737007f44", "6f4f195712bd76a67b2cba6737008c8a", "6f4f195712bd76a67b2cba6737009adb" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(uuidsResult);

        Response response = CUT.getUuids(auth, 3);
        assertEquals(200, response.getStatus());
        assertEquals(uuidsResult, response.getEntity());
        assertEquals("Gold", response.getHeaderString("AppServer")); // third color
        assertEquals("Crimson", response.getHeaderString("DbServer")); // second color
    }

    @Test
    public void getUuids_moreMembersThanColors() {
        UuidsResult uuidsResult = new UuidsResult();
        uuidsResult.addHeader(HttpHeaders.BACKEND, "192.168.0.5"); // last color
        uuidsResult.setUuids(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737007f44", "6f4f195712bd76a67b2cba6737008c8a", "6f4f195712bd76a67b2cba6737009adb",
                                                          "6f4f195712bd76a67b2cba6737010334", "6f4f195712bd76a67b2cba6737aa037b", "6f4f195712bd76a67b2cba67478df2ac" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(uuidsResult);

        Set<Member> members = createMembers(6);
        Cluster cluster = createCluster(members, "db6.domain.de"); // sixth AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn(password.toCharArray());
        CUT = new UuidsFacade(uuidsService, pool, hazelcast, dnsResolver, lbConfig);

        Response response = CUT.getUuids(auth, 6);
        assertEquals(200, response.getStatus());
        assertEquals(uuidsResult, response.getEntity());
        assertEquals("Black", response.getHeaderString("AppServer")); // default color
        assertEquals("LightSalmon", response.getHeaderString("DbServer")); // last color
    }

    @Test
    public void getUuids_moreDbServersThanColors() {
        UuidsResult uuidsResult = new UuidsResult();
        uuidsResult.setUuids(Arrays.asList(new String[] { "c8fedfee503b1de6d52e3a52e10be656" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(uuidsResult);

        for (int j = 1; j <= JAXRSConfiguration.COLORS.size(); j++) {
            uuidsResult.setHeaders(new HashMap<>());
            uuidsResult.addHeader(HttpHeaders.BACKEND, "192.168.0." + j);
            Response response = CUT.getUuids(auth, 1);
            assertEquals(JAXRSConfiguration.COLORS.get(j - 1), response.getHeaderString("DbServer"));
        }
        uuidsResult.setHeaders(new HashMap<>());
        uuidsResult.addHeader(HttpHeaders.BACKEND, "192.168.0." + 6); // overflow
        Response response = CUT.getUuids(auth, 1);
        assertEquals(JAXRSConfiguration.DEFAULT_COLOR, response.getHeaderString("DbServer"));
    }

}
