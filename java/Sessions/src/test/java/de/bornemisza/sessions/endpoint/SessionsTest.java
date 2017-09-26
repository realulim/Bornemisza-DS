package de.bornemisza.sessions.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

import org.javalite.http.Get;
import org.javalite.http.Post;

import de.bornemisza.rest.Http;
import de.bornemisza.rest.da.HttpPool;
import de.bornemisza.rest.entity.Session;

public class SessionsTest {

    private final String AUTH_HEADER = "Basic RmF6aWwgT25ndWRhcjpjaGFuZ2Vk";

    private Sessions CUT;
    private Http http;
    private Post post;
    private Get get;
    private final Map<String, List<String>> headers = new HashMap<>();
    private HazelcastInstance hazelcast;

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
        when(http.getBaseUrl()).thenReturn("http://db2.domain.de/foo"); // second DbServer

        HttpPool pool = mock(HttpPool.class);
        when(pool.getConnection()).thenReturn(http);
        Map<String, Http> allConnections = new HashMap<>();
        allConnections.put("db1.domain.ms", http);
        allConnections.put("db2.domain.de", http);
        allConnections.put("db3.domain.com", http);
        when(pool.getAllConnections()).thenReturn(allConnections);

        hazelcast = mock(HazelcastInstance.class);
        Cluster cluster = mock(Cluster.class);
        Set<Member> members = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Member member = mock(Member.class);
            when(member.getUuid()).thenReturn(i + "");
            members.add(member);
        }
        when(cluster.getMembers()).thenReturn(members);
        Member myself = mock(Member.class);
        when(cluster.getLocalMember()).thenReturn(myself);
        when(myself.getUuid()).thenReturn("2"); // third AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);

        CUT = new Sessions(pool, pool, hazelcast);
    }

    @Test
    public void getNewSession_authHeaderMissing() {
        try {
            CUT.getNewSession(null);
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(401, e.getResponse().getStatus());
        }
    }

    @Test
    public void getNewSession_postFailed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(post.responseCode()).thenReturn(errorCode);
        when(post.responseMessage()).thenReturn(msg);
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(errorCode, response.getStatus());
        assertEquals(msg, response.getEntity());
    }

    @Test
    public void getNewSession_noCookie() {
        when(post.responseCode()).thenReturn(200);
        headers.put(HttpHeaders.SET_COOKIE, new ArrayList<>());
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(500, response.getStatus());
        assertEquals("No Cookie!", response.getEntity());
    }

    @Test
    public void getNewSession() {
        String cookie = "My Cookie";
        when(post.responseCode()).thenReturn(200);
        List<String> cookies = new ArrayList<>();
        cookies.add(cookie);
        headers.put(HttpHeaders.SET_COOKIE, cookies);
        Response response = CUT.getNewSession(AUTH_HEADER);
        assertEquals(200, response.getStatus());
        assertEquals(cookie, response.getHeaderString("C-Token"));
    }

    @Test
    public void getActiveSession_noCookie() {
        try {
            CUT.getActiveSession("null");
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(401, e.getResponse().getStatus());
            assertEquals("No Cookie!", e.getResponse().getEntity());
        }
    }

    @Test
    public void getActiveSession_getFailed() {
        int errorCode = 509;
        String msg = "Bandwidth Limit Exceeded";
        when(get.responseCode()).thenReturn(errorCode);
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.getActiveSession("MyCookie");
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(errorCode, e.getResponse().getStatus());
            assertEquals(msg, e.getResponse().getEntity());
        }
    }

    @Test
    public void getActiveSession_noJson() {
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn("not Json");
        try {
            CUT.getActiveSession("MyCookie");
            fail();
        }
        catch (WebApplicationException e) {
            assertEquals(500, e.getResponse().getStatus());
        }
    }

    @Test
    public void getActiveSession() {
        String json = "{\"ok\":true,\"userCtx\":{\"name\":\"Fazil Ongudar\",\"roles\":[\"customer\",\"user\"]},\"info\":{\"authentication_db\":\"_users\",\"authentication_handlers\":[\"cookie\",\"default\"],\"authenticated\":\"cookie\"}}";
        String cookie = "MyCookie";
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(json);
        Session session = CUT.getActiveSession(cookie);
        assertEquals("Fazil Ongudar", session.getPrincipal());
        assertEquals(cookie, session.getCToken());
    }

    @Test
    public void deleteCookieInBrowser() {
        Response response = CUT.deleteCookieInBrowser("MyCookie");
        assertEquals(200, response.getStatus());
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

}
