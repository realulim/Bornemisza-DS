package de.bornemisza.sessions.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.nio.Address;

import org.javalite.http.Get;
import org.javalite.http.Post;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.bornemisza.rest.Http;
import de.bornemisza.sessions.JAXRSConfiguration;
import de.bornemisza.sessions.da.DnsResolver;
import de.bornemisza.sessions.da.HttpBasePool;

public class UuidsTest {

    private Uuids CUT;
    private Http http;
    private Post post;
    private Get get;
    private final Map<String, List<String>> headers = new HashMap<>();
    private HazelcastInstance hazelcast;
    private HttpBasePool pool;
    private DnsResolver dnsResolver;
    private final List<String> ipAddresses = new ArrayList<>();
    private final Map<String, List<String>> mapWithBackendHeader = new HashMap<>();

    @Before
    public void setUp() {
        post = mock(Post.class);
        when(post.param(anyString(), any())).thenReturn(post);
        when(post.headers()).thenReturn(headers);
        http = mock(Http.class);
        when(http.post(anyString())).thenReturn(post);

        get = mock(Get.class);
        when(get.header(anyString(), any())).thenReturn(get);
        when(http.get(anyString())).thenReturn(get);
        when(http.getBaseUrl()).thenReturn("http://db1.domain.de/foo"); // second DbServer

        pool = mock(HttpBasePool.class);
        when(pool.getConnection()).thenReturn(http);

        hazelcast = mock(HazelcastInstance.class);
        Set<Member> members = createMembers(5);
        Cluster cluster = createCluster(members, "3"); // third AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);

        this.dnsResolver = mock(DnsResolver.class);
        when(dnsResolver.getHostAddress(anyString())).thenAnswer(new IpAddressAnswer());

        CUT = new Uuids(pool, hazelcast, dnsResolver);
    }

    class IpAddressAnswer implements Answer {
        private int i = 0;
        @Override public String answer(InvocationOnMock iom) throws Throwable {
            String ipAddress = ipAddresses.get(i++);
            return ipAddress;
        }
    }

    private Set<Member> createMembers(int count) {
        Map<String, Http> allConnections = new HashMap<>();
        Set<Member> members = new HashSet<>();
        for (int i = 1; i <= count; i++) {
            String hostname = "db" + i + ".domain.de";
            allConnections.put(hostname, http);
            ipAddresses.add("192.168.0." + i);

            Member member = mock(Member.class);
            when(member.getUuid()).thenReturn(i + "");
            Address address = mock(Address.class);
            when(address.getHost()).thenReturn(hostname);
            when(member.getAddress()).thenReturn(address);
            members.add(member);
        }
        when(pool.getAllHostnames()).thenReturn(allConnections.keySet());
        return members;
    }

    private Cluster createCluster(Set<Member> members, String myUuid) {
        Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(members);
        Member myself = mock(Member.class);
        when(cluster.getLocalMember()).thenReturn(myself);
        when(myself.getUuid()).thenReturn(myUuid);
        return cluster;
    }

    @Test
    public void getUuids_noCookie() {
        try {
            CUT.getUuids("", 3);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(401, e.getResponse().getStatus());
            assertEquals("No Cookie!", e.getResponse().getEntity());
        }
    }

    @Test
    public void getUuids_getFailed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(get.responseCode()).thenReturn(errorCode);
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.getUuids("MyCookie", 3);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(errorCode, e.getResponse().getStatus());
            assertEquals(msg, e.getResponse().getEntity());
        }
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
        CUT = new Uuids(pool, hazelcast, dnsResolver);

        String cookie = "MyCookie";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0.5" }));
        when(get.headers()).thenReturn(mapWithBackendHeader);
        Response response = CUT.getUuids(cookie, 1);
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
        String cookie = "MyCookie";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0." + 2 })); // second color
        when(get.headers()).thenReturn(mapWithBackendHeader);
        Response response = CUT.getUuids(cookie, 3);
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
        String cookie = "MyCookie";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0.5" })); // last color
        when(get.headers()).thenReturn(mapWithBackendHeader);

        Set<Member> members = createMembers(6);
        Cluster cluster = createCluster(members, "6"); // sixth AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);
        CUT = new Uuids(pool, hazelcast, dnsResolver);

        when(http.getBaseUrl()).thenReturn("http://db4.domain.de/foo"); // fifth DbServer

        Response response = CUT.getUuids(cookie, 6);
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
        String cookie = "MyCookie";
        when(get.text()).thenReturn(json);
        when(get.headers()).thenReturn(mapWithBackendHeader);
        for (int j = 1; j <= JAXRSConfiguration.COLORS.size(); j++) {
            mapWithBackendHeader.clear();
            mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0." + j }));
            Response response = CUT.getUuids(cookie, 1);
            assertEquals(JAXRSConfiguration.COLORS.get(j - 1), response.getHeaderString("DbServer"));
        }
        mapWithBackendHeader.clear();
        mapWithBackendHeader.put("X-Backend", Arrays.asList(new String[] { "192.168.0." + 6 })); // overflow
        Response response = CUT.getUuids(cookie, 1);
        assertEquals(JAXRSConfiguration.DEFAULT_COLOR, response.getHeaderString("DbServer"));
    }

}
