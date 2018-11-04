package de.bornemisza.ds.rest.security;

import de.bornemisza.ds.rest.security.HashProvider;
import de.bornemisza.ds.rest.security.DoubleSubmitToken;
import de.bornemisza.ds.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.ds.rest.security.Auth;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.ds.rest.exception.UnauthorizedException;

public class AuthTest {

    private Auth CUT;
    private HashProvider hashProvider;

    public AuthTest() {
    }
    
    @Before
    public void setUp() {
        LoadBalancerConfig lbConfig = mock(LoadBalancerConfig.class);
        when(lbConfig.getPassword()).thenReturn("My Super Password".toCharArray());
        hashProvider = new DbAdminPasswordBasedHashProvider(lbConfig);
    }

    @Test(expected=UnauthorizedException.class)
    public void checkTokenValidity_cookieVoid() {
        String cookie = "";
        String cToken = hashProvider.encodeJasonWebToken("Hirono Nikasati");
        DoubleSubmitToken dsToken = new DoubleSubmitToken(cookie, cToken);
        CUT = new Auth(dsToken);
        CUT.checkTokenValidity(hashProvider);
    }

    @Test(expected=UnauthorizedException.class)
    public void checkTokenValidity_cTokenVoid() {
        String cookie = "AuthSession=b866f6e2-be02-4ea0-99e6-34f989629930";
        DoubleSubmitToken dsToken = new DoubleSubmitToken(cookie, null);
        CUT = new Auth(dsToken);
        CUT.checkTokenValidity(hashProvider);
    }

    @Test
    public void checkTokenValidity() {
        String cookie = "AuthSession=RmF6aWwgT25ndWRhcjo1QTM2Nzc5Rg==";
        String cToken = hashProvider.encodeJasonWebToken("Fazil Ongudar");
        DoubleSubmitToken dsToken = new DoubleSubmitToken(cookie, cToken);
        CUT = new Auth(dsToken);
        CUT.checkTokenValidity(hashProvider);
    }
    
}
