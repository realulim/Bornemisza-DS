package de.bornemisza.sessions.facade;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.javalite.http.Post;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.sessions.JAXRSConfiguration;
import de.bornemisza.sessions.boundary.UuidsFacade;
import de.bornemisza.sessions.da.CouchPool;
import de.bornemisza.sessions.da.DnsResolver;
import de.bornemisza.sessions.security.DbAdminPasswordBasedHashProvider;

public class UuidsFacadeTest {

    private UuidsFacade CUT;
    private Http http;
    private HttpConnection conn;
    private Post post;
    private Get get;
    private final Map<String, List<String>> headers = new HashMap<>();
    private HazelcastInstance hazelcast;
    private CouchPool pool;
    private DnsResolver dnsResolver;
    private final List<String> ipAddresses = new ArrayList<>();
    private final Map<String, List<String>> mapWithBackendHeader = new HashMap<>();
    private DbAdminPasswordBasedHashProvider hashProvider;
    private String cookie, hmac;
    private DoubleSubmitToken dsToken;

    @Before
    public void setUp() {
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn("My Secret Password".toCharArray());
        hashProvider = new DbAdminPasswordBasedHashProvider(lbConfig);
        cookie = "MyCookie";
        hmac = hashProvider.hmacDigest(cookie);
        dsToken = new DoubleSubmitToken(cookie, hmac);

        post = mock(Post.class);
        when(post.param(anyString(), any())).thenReturn(post);
        when(post.headers()).thenReturn(headers);
        http = mock(Http.class);
        when(http.post(anyString())).thenReturn(post);

        get = mock(Get.class);
        when(get.header(anyString(), any())).thenReturn(get);
        when(http.get(anyString())).thenReturn(get);
        when(http.getBaseUrl()).thenReturn("http://db1.domain.de/foo"); // second DbServer

        pool = mock(CouchPool.class);
        conn = mock(HttpConnection.class);
        when(conn.getHttp()).thenReturn(http);
        when(pool.getConnection()).thenReturn(conn);

        hazelcast = mock(HazelcastInstance.class);
        Set<Member> members = createMembers(5);
        Cluster cluster = createCluster(members, "db3.domain.de"); // third AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);

        this.dnsResolver = mock(DnsResolver.class);
        when(dnsResolver.getHostAddress(anyString())).thenAnswer(new IpAddressAnswer());

        CUT = new UuidsFacade(pool, hazelcast, dnsResolver, hashProvider);
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
            allConnections.put(hostname, conn);
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
            CUT.getUuids(new DoubleSubmitToken("", ""), 3);
            fail();
        }
        catch (UnauthorizedException e) {
            assertEquals("Cookie or C-Token missing!", e.getMessage());
        }
    }

    @Test
    public void getUuids_hashMismatch() {
        try {
            CUT.getUuids(new DoubleSubmitToken(cookie, "this-is-not-a-hash-function"), 1);
            fail();
        }
        catch (UnauthorizedException e) {
            assertEquals("Hash Mismatch!", e.getMessage());
        }
    }

    @Test
    public void getUuids_technicalError() {
        String msg = "Connection refused";
        ConnectException cause = new ConnectException(msg);
        HttpException wrapperException = new HttpException(msg, cause);
        when(get.responseCode()).thenThrow(wrapperException);
        Response resp = CUT.getUuids(dsToken, 3);
        assertEquals(500, resp.getStatus());
        assertEquals(wrapperException.toString(), resp.getEntity());
    }

    @Test
    public void getUuids_getFailed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(get.responseCode()).thenReturn(errorCode);
        when(get.responseMessage()).thenReturn(msg);
        Response resp = CUT.getUuids(dsToken, 3);
        assertEquals(errorCode, resp.getStatus());
        assertEquals(msg, resp.getEntity());
    }

    @Test
    public void getUuids_unresolvableHostname() {
        String json = "{\n" +
                      "    \"uuids\": [\n" +
                      "        \"6f4f195712bd76a67b2cba6737009adb\"\n" +
                      "    ]\n" +
                      "}";
        this.dnsResolver = mock(DnsResolver.class);
        when(dnsResolver.getHostAddress(anyString())).thenReturn(null);
        CUT = new UuidsFacade(pool, hazelcast, dnsResolver, hashProvider);

        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0.5" }));
        when(get.headers()).thenReturn(mapWithBackendHeader);
        Response response = CUT.getUuids(dsToken, 1);
        assertEquals(200, response.getStatus());
        assertEquals(json, response.getEntity());
        assertEquals("Black", response.getHeaderString("DbServer")); // second color
    }

    @Test
    public void getUuids() {
        String json = "{\n" +
                      "    \"uuids\": [\n" +
                      "        \"6f4f195712bd76a67b2cba6737007f44\",\n" +
                      "        \"6f4f195712bd76a67b2cba6737008c8a\",\n" +
                      "        \"6f4f195712bd76a67b2cba6737009adb\"\n" +
                      "    ]\n" +
                      "}";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0." + 2 })); // second color
        when(get.headers()).thenReturn(mapWithBackendHeader);
        Response response = CUT.getUuids(dsToken, 3);
        assertEquals(200, response.getStatus());
        assertEquals(json, response.getEntity());
        assertEquals("Gold", response.getHeaderString("AppServer")); // third color
        assertEquals("Crimson", response.getHeaderString("DbServer")); // second color
    }

    @Test
    public void getUuids_moreMembersThanColors() {
        String json = "{\n" +
                      "    \"uuids\": [\n" +
                      "        \"6f4f195712bd76a67b2cba6737007f44\",\n" +
                      "        \"6f4f195712bd76a67b2cba6737008c8a\",\n" +
                      "        \"6f4f195712bd76a67b2cba6737009adb\",\n" +
                      "        \"6f4f195712bd76a67b2cba6737010334\",\n" +
                      "        \"6f4f195712bd76a67b2cba6737aa037b\",\n" +
                      "        \"6f4f195712bd76a67b2cba67478df2ac\"\n" +
                      "    ]\n" +
                      "}";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0.5" })); // last color
        when(get.headers()).thenReturn(mapWithBackendHeader);

        Set<Member> members = createMembers(6);
        Cluster cluster = createCluster(members, "db6.domain.de"); // sixth AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);
        CUT = new UuidsFacade(pool, hazelcast, dnsResolver, hashProvider);

        when(http.getBaseUrl()).thenReturn("http://db4.domain.de/foo"); // fifth DbServer

        Response response = CUT.getUuids(dsToken, 6);
        assertEquals(200, response.getStatus());
        assertEquals(json, response.getEntity());
        assertEquals("Black", response.getHeaderString("AppServer")); // default color
        assertEquals("LightSalmon", response.getHeaderString("DbServer")); // last color
    }

    @Test
    public void getUuids_moreDbServersThanColors() {
        when(get.responseCode()).thenReturn(200);
        String json = "{\n" +
                      "    \"uuids\": [\n" +
                      "        \"c8fedfee503b1de6d52e3a52e10be656\"\n" +
                      "    ]\n" +
                      "}";
        when(get.text()).thenReturn(json);
        when(get.headers()).thenReturn(mapWithBackendHeader);
        for (int j = 1; j <= JAXRSConfiguration.COLORS.size(); j++) {
            mapWithBackendHeader.clear();
            mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0." + j }));
            Response response = CUT.getUuids(dsToken, 1);
            assertEquals(JAXRSConfiguration.COLORS.get(j - 1), response.getHeaderString("DbServer"));
        }
        mapWithBackendHeader.clear();
        mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0." + 6 })); // overflow
        Response response = CUT.getUuids(dsToken, 1);
        assertEquals(JAXRSConfiguration.DEFAULT_COLOR, response.getHeaderString("DbServer"));
    }

}
