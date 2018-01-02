package de.bornemisza.sessions.boundary;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
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
import de.bornemisza.rest.Json;
import de.bornemisza.rest.entity.Uuid;
import de.bornemisza.rest.entity.result.KeyValueViewResult;
import de.bornemisza.rest.entity.result.UuidsResult;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.rest.security.HashProvider;
import de.bornemisza.sessions.JAXRSConfiguration;
import de.bornemisza.sessions.consumer.StoreUuidRequest;
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
    private IQueue uuidWriteQueue;

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

        this.uuidWriteQueue = mock(IQueue.class);
        when(hazelcast.getQueue(anyString())).thenReturn(uuidWriteQueue);

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
        UuidsResult dbResult = new UuidsResult();
        dbResult.addHeader(HttpHeaders.BACKEND, "192.168.0.5");
        dbResult.setUuids(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737009adb" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(dbResult);
        when(uuidsService.saveUuids(any(Auth.class), anyString(), any(Uuid.class))).thenReturn(dbResult);
        CUT = new UuidsFacade(uuidsService, pool, hazelcast, dnsResolver, lbConfig);

        UuidsResult facadeResult = CUT.getUuids(auth, 1);
        assertEquals(200, facadeResult.getStatus().getStatusCode());
        assertEquals(dbResult.getUuids(), facadeResult.getUuids());
        assertEquals("Black", facadeResult.getFirstHeaderValue(HttpHeaders.DBSERVER)); // second color
    }

    @Test
    public void getUuids() {
        UuidsResult dbResult = new UuidsResult();
        dbResult.addHeader(HttpHeaders.BACKEND, "192.168.0." + 2); // second color
        dbResult.setUuids(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737007f44", "6f4f195712bd76a67b2cba6737008c8a", "6f4f195712bd76a67b2cba6737009adb" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(dbResult);
        when(uuidsService.saveUuids(any(Auth.class), anyString(), any(Uuid.class))).thenReturn(dbResult);

        UuidsResult facadeResult = CUT.getUuids(auth, 3);
        assertEquals(200, facadeResult.getStatus().getStatusCode());
        assertEquals(dbResult.getUuids(), facadeResult.getUuids());
        assertEquals("Gold", facadeResult.getFirstHeaderValue(HttpHeaders.APPSERVER)); // third color
        assertEquals("Crimson", facadeResult.getFirstHeaderValue(HttpHeaders.DBSERVER)); // second color

        verify(uuidsService).saveUuids(any(Auth.class), anyString(), any(Uuid.class));
    }

//    @Test
    public void getUuids_notQueued() {
        when(uuidWriteQueue.offer(any(StoreUuidRequest.class))).thenReturn(false);
        UuidsResult dbResult = new UuidsResult();
        dbResult.addHeader(HttpHeaders.BACKEND, "192.168.0." + 2); // second color
        dbResult.setUuids(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737007f44", "6f4f195712bd76a67b2cba6737008c8a", "6f4f195712bd76a67b2cba6737009adb" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(dbResult);
        when(uuidsService.saveUuids(any(Auth.class), anyString(), any(Uuid.class))).thenReturn(dbResult);

        UuidsResult facadeResult = CUT.getUuids(auth, 3);
        assertEquals(200, facadeResult.getStatus().getStatusCode());
        assertEquals(dbResult.getUuids(), facadeResult.getUuids());
        assertEquals("Gold", facadeResult.getFirstHeaderValue(HttpHeaders.APPSERVER)); // third color
        assertEquals("Crimson", facadeResult.getFirstHeaderValue(HttpHeaders.DBSERVER)); // second color

        verify(uuidsService).saveUuids(any(Auth.class), anyString(), any(Uuid.class));
    }

//    @Test
    public void getUuids_queued() {
        when(uuidWriteQueue.offer(any(StoreUuidRequest.class))).thenReturn(true);
        UuidsResult dbResult = new UuidsResult();
        dbResult.addHeader(HttpHeaders.BACKEND, "192.168.0." + 2); // second color
        dbResult.setUuids(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737007f44", "6f4f195712bd76a67b2cba6737008c8a", "6f4f195712bd76a67b2cba6737009adb" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(dbResult);

        UuidsResult facadeResult = CUT.getUuids(auth, 3);
        assertEquals(200, facadeResult.getStatus().getStatusCode());
        assertEquals(dbResult.getUuids(), facadeResult.getUuids());
        assertEquals("Gold", facadeResult.getFirstHeaderValue(HttpHeaders.APPSERVER)); // third color
        assertEquals("Crimson", facadeResult.getFirstHeaderValue(HttpHeaders.DBSERVER)); // second color
    }

    @Test
    public void getUuids_moreMembersThanColors() {
        UuidsResult dbResult = new UuidsResult();
        dbResult.addHeader(HttpHeaders.BACKEND, "192.168.0.5"); // last color
        dbResult.setUuids(Arrays.asList(new String[] { "6f4f195712bd76a67b2cba6737007f44", "6f4f195712bd76a67b2cba6737008c8a", "6f4f195712bd76a67b2cba6737009adb",
                                                          "6f4f195712bd76a67b2cba6737010334", "6f4f195712bd76a67b2cba6737aa037b", "6f4f195712bd76a67b2cba67478df2ac" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(dbResult);
        when(uuidsService.saveUuids(any(Auth.class), anyString(), any(Uuid.class))).thenReturn(dbResult);

        Set<Member> members = createMembers(6);
        Cluster cluster = createCluster(members, "db6.domain.de"); // sixth AppServer
        when(hazelcast.getCluster()).thenReturn(cluster);
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn(password.toCharArray());
        CUT = new UuidsFacade(uuidsService, pool, hazelcast, dnsResolver, lbConfig);

        UuidsResult facadeResult = CUT.getUuids(auth, 6);
        assertEquals(200, facadeResult.getStatus().getStatusCode());
        assertEquals(dbResult.getUuids(), facadeResult.getUuids());
        assertEquals("Black", facadeResult.getFirstHeaderValue(HttpHeaders.APPSERVER)); // default color
        assertEquals("LightSalmon", facadeResult.getFirstHeaderValue(HttpHeaders.DBSERVER)); // last color
    }

    @Test
    public void getUuids_moreDbServersThanColors() {
        UuidsResult dbResult = new UuidsResult();
        dbResult.setUuids(Arrays.asList(new String[] { "c8fedfee503b1de6d52e3a52e10be656" }));
        when(uuidsService.getUuids(anyInt())).thenReturn(dbResult);
        when(uuidsService.saveUuids(any(Auth.class), anyString(), any(Uuid.class))).thenReturn(dbResult);

        for (int j = 1; j <= JAXRSConfiguration.COLORS.size(); j++) {
            dbResult = createUuidsResult(j);
            when(uuidsService.getUuids(anyInt())).thenReturn(dbResult);
            UuidsResult facadeResult = CUT.getUuids(auth, 1);
            assertEquals(JAXRSConfiguration.COLORS.get(j - 1), facadeResult.getFirstHeaderValue(HttpHeaders.DBSERVER));
        }
        dbResult = createUuidsResult(6); // overflow
        when(uuidsService.getUuids(anyInt())).thenReturn(dbResult);
        UuidsResult facadeResult = CUT.getUuids(auth, 1);
        assertEquals(JAXRSConfiguration.DEFAULT_COLOR, facadeResult.getFirstHeaderValue(HttpHeaders.DBSERVER));
    }

    @Test
    public void loadColors() {
        KeyValueViewResult dbResult = createColorsResult();
        when(uuidsService.loadColors(any(Auth.class), anyString())).thenReturn(dbResult);

        KeyValueViewResult facadeResult = CUT.loadColors(auth);
        assertEquals(200, facadeResult.getStatus().getStatusCode());
        assertEquals(dbResult, facadeResult);
    }

    private KeyValueViewResult createColorsResult() {
        String json =   " {\"rows\":[\n" +
                        "        {\"key\":\"Black\",\"value\":145},\n" +
                        "        {\"key\":\"Crimson\",\"value\":2846},\n" +
                        "        {\"key\":\"LightSeaGreen\",\"value\":8087}\n" +
                        " ]}\n";
        return Json.fromJson(json, KeyValueViewResult.class);
    }

    private UuidsResult createUuidsResult(int j) throws BusinessException, TechnicalException {
        UuidsResult dbResult = new UuidsResult();
        dbResult.addHeader(HttpHeaders.BACKEND, "192.168.0." + j);
        dbResult.setUuids(Arrays.asList(new String[] { "c8fedfee503b1de6d52e3a52e10be656" }));
        return dbResult;
    }

}
