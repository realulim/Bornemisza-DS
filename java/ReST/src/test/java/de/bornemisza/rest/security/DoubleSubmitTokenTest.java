package de.bornemisza.rest.security;

import org.junit.Before;
import org.junit.Test;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.domain.JWTException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.exception.UnauthorizedException;

public class DoubleSubmitTokenTest {

    private DoubleSubmitToken CUT;
    private HashProvider hashProvider;
    private final String cookie = "AuthSession=RmF6aWwgT25ndWRhcjo1QTM2Nzc5Rg==";
    private final String cookieWithoutKey = "RmF6aWwgT25ndWRhcjo1QTM2Nzc5Rg==";
    private final String cookieWithoutPrincipal = "AuthSession=b866f6e2-be02-4ea0-99e6-34f989629930; Version=1; Path=/; HttpOnly; Secure";
    private final String cToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJIaXJvbm8gTmlrYXNhdGkiLCJDb29raWUiOiJBdXRoU2Vzc2lvbj1iODY2ZjZlMi1iZTAyLTRlYTAtOTllNi0zNGY5ODk2Mjk5MzAifQ.pErlMn2UFY6kL8Ft_DR9InZOC-EeD2-z3_ZdG6uMKRI";

    public DoubleSubmitTokenTest() {
    }
    
    @Before
    public void setUp() {
        hashProvider = mock(HashProvider.class);
    }

    @Test(expected=UnauthorizedException.class)
    public void checkTokenValidity_cookieVoid() {
        CUT = new DoubleSubmitToken(null, "cToken");
        CUT.checkValidity(hashProvider);
    }

    @Test(expected=UnauthorizedException.class)
    public void checkTokenValidity_cTokenVoid() {
        CUT = new DoubleSubmitToken("Cookie", "");
        CUT.checkValidity(hashProvider);
    }

    @Test
    public void checkValidity_invalidJWT() {
        CUT = new DoubleSubmitToken(cookie, cToken);
        when(hashProvider.decodeJasonWebToken(anyString())).thenThrow(new JWTException("invalid JWT - cannot decode"));
        try {
            CUT.checkValidity(hashProvider);
            fail();
        }
        catch (UnauthorizedException ex) {
            assertTrue(ex.getMessage().startsWith("JWT invalid:"));
        }
    }

    @Test
    public void checkValidity_cookieWithoutPrincipal() {
        CUT = new DoubleSubmitToken(cookieWithoutPrincipal, cToken);
        try {
            CUT.checkValidity(hashProvider);
            fail();
        }
        catch (UnauthorizedException ex) {
            assertEquals("Cookie invalid!", ex.getMessage());
        }
    }

    @Test
    public void checkValidity_jwtWithoutSubject() {
        CUT = new DoubleSubmitToken(cookie, cToken);
        JWT jwt = new JWT();
        when(hashProvider.decodeJasonWebToken(anyString())).thenReturn(jwt);
        try {
            CUT.checkValidity(hashProvider);
            fail();
        }
        catch (UnauthorizedException ex) {
            assertEquals("JWT invalid!", ex.getMessage());
        }
    }

    @Test
    public void checkValidity_principalSubjectMismatch() {
        CUT = new DoubleSubmitToken(cookie, cToken);
        JWT jwt = new JWT();
        jwt.setSubject("Someone else");
        when(hashProvider.decodeJasonWebToken(anyString())).thenReturn(jwt);
        try {
            CUT.checkValidity(hashProvider);
        }
        catch (UnauthorizedException ex) {
            assertEquals("Hash Mismatch!", ex.getMessage());
        }
    }

    @Test
    public void checkValidity() {
        String principal = "Fazil Ongudar";
        CUT = new DoubleSubmitToken(cookie, cToken);
        JWT jwt = new JWT();
        jwt.setSubject(principal);
        when(hashProvider.decodeJasonWebToken(anyString())).thenReturn(jwt);
        String userName = CUT.checkValidity(hashProvider);
        assertEquals(principal, userName);

        CUT = new DoubleSubmitToken(cookieWithoutKey, cToken);
        userName = CUT.checkValidity(hashProvider);
        assertEquals(principal, userName);
    }

}
