package de.bornemisza.sessions.endpoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.javalite.http.Get;
import org.javalite.http.Post;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.nio.Address;

import de.bornemisza.rest.Http;
import de.bornemisza.rest.da.HttpPool;
import de.bornemisza.sessions.JAXRSConfiguration;

public class UuidsTest {

    private Uuids CUT;
    private Http http;
    private Post post;
    private Get get;
    private final Map<String, List<String>> headers = new HashMap<>();
    private HazelcastInstance hazelcast;
    private HttpPool pool;

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

        pool = mock(HttpPool.class);
        when(pool.getConnection()).thenReturn(http);

        hazelcast = mock(HazelcastInstance.class);
        Set<Member> members = createMembers(5);
        Cluster cluster = createCluster(members, "2"); // third AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);

        CUT = new Uuids(pool, hazelcast);
    }

    private Set<Member> createMembers(int count) {
        Map<String, Http> allConnections = new HashMap<>();
        Set<Member> members = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String hostname = "db" + i + ".domain.de";
            allConnections.put(hostname, http);
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
    public void getUuids_unexpectedHostname() {
        String unexpectedHostname = "have.this.not";
        when(http.getBaseUrl()).thenReturn("http://" + unexpectedHostname + "/foo");
        when(get.responseCode()).thenReturn(200);
        try {
            CUT.getUuids("MyCookie", 3);
        }
        catch (WebApplicationException e) {
            assertTrue(e.getResponse().getEntity().toString().startsWith("Hostname " + unexpectedHostname + " not found"));
        }
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

        Set<Member> members = createMembers(6);
        Cluster cluster = createCluster(members, "5"); // sixth AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);
        CUT = new Uuids(pool, hazelcast);

        when(http.getBaseUrl()).thenReturn("http://db4.domain.de/foo"); // fifth DbServer

        Response response = CUT.getUuids(cookie, 6);
        assertEquals(200, response.getStatus());
        assertEquals(json, response.getEntity());
        assertEquals("Black", response.getHeaderString("AppServer")); // default color
        assertEquals("LightSalmon", response.getHeaderString("DbServer")); // last color
    }

//    @Test
//    public void getUuids() {
//        String firstJson = "{\n" +
//                      "    \"uuids\": [\n" +
//                      "        \"8d0866239666bb5ad6fc5dce8297eef7\",\n" +
//                      "        \"c8fedfee503b1e6d52e3a52e10be5d1b\",\n" +
//                      "        \"8d0866239666bb5ad6fc5dce8297fe70\"\n" +
//                      "    ]\n" +
//                      "}";
//        String secondJson = "{\n" +
//                      "    \"uuids\": [\n" +
//                      "        \"c8fedfee503b1e6d52e3a52e10be6560\"\n" +
//                      "    ]\n" +
//                      "}";
//        String thirdJson = "{\n" +
//                      "    \"uuids\": [\n" +
//                      "        \"8d0866239666bb5ad6fc5dce829809fa\"\n" +
//                      "    ]\n" +
//                      "}";
//        String cookie = "MyCookie";
//        when(get.responseCode()).thenReturn(200);
//        when(get.text()).thenReturn(firstJson).thenReturn(secondJson).thenReturn(thirdJson);
//        Response response = CUT.getUuids(cookie, 3);
//        assertEquals(200, response.getStatus());
//        assertEquals(firstJson, response.getEntity());
//        assertEquals("Gold", response.getHeaderString("AppServer")); // third color
//        
//        response = CUT.getUuids(cookie, 1);
//        assertEquals("Crimson", response.getHeaderString("DbServer")); // second color
//        response = CUT.getUuids(cookie, 1);
//        assertEquals("LightSeaGreen", response.getHeaderString("DbServer")); // first color
//    }
//
//    @Test
//    public void getUuids_moreDbServersThanColors() {
//        when(get.responseCode()).thenReturn(200);
//        String cookie = "Cookie";
//        when(get.text()).thenReturn(getJsonResponse(0)).thenReturn(getJsonResponse(1))
//                .thenReturn(getJsonResponse(2)).thenReturn(getJsonResponse(3))
//                .thenReturn(getJsonResponse(4)).thenReturn(getJsonResponse(5));
//        for (int j = 0; j < JAXRSConfiguration.COLORS.size(); j++) {
//            Response response = CUT.getUuids(cookie, 1);
//            System.out.println(response.getHeaderString("DbServer"));
//            assertEquals(JAXRSConfiguration.COLORS.get(j), response.getHeaderString("DbServer"));
//        }
//        Response response = CUT.getUuids(cookie, 1);
//        assertEquals(JAXRSConfiguration.DEFAULT_COLOR, response.getHeaderString("DbServer"));
//    }
//
//    private String getJsonResponse(int prefix) {
//        return "{\n"
//               + "    \"uuids\": [\n"
//               + "        \"" + prefix + "c8fedfee503b1e6d52e3a52e10be656\"\n"
//               + "    ]\n"
//               + "}";
//    }

}
