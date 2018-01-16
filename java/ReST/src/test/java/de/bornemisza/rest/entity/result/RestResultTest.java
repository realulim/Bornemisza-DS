package de.bornemisza.rest.entity.result;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.EmailAddress;
import de.bornemisza.rest.entity.User;

public class RestResultTest {
    
    @Before
    public void setUp() {
    }

    @Test
    public void addSingleHeaderValue() {
        String key = HttpHeaders.BACKEND;
        String value = "1.2.3.4";
        RestResult CUT = new RestResult();
        CUT.addHeader(key, value);
        assertEquals(value, CUT.getFirstHeaderValue(key));
    }

    @Test
    public void addMultipleHeaderValues() {
        String key = HttpHeaders.BACKEND;
        String firstValue = "1.2.3.4";
        String[] values = new String[] { firstValue, "2.3.4.5", "6.5.4.3" };
        RestResult CUT = new RestResult();
        CUT.addHeader(key, values);
        assertEquals(firstValue, CUT.getFirstHeaderValue(key));
        assertEquals(Arrays.asList(values), CUT.getHeaders().get(key));
    }

    @Test
    public void addHeaderFrom() {
        String key = HttpHeaders.BACKEND;
        String firstValue = "1.2.3.4";
        Map<String, List<String>> headers = new HashMap<>();
        List<String>values = Arrays.asList(new String[] { firstValue, "2.3.4.5", "6.5.4.3" });
        headers.put(key, values);
        headers.put("otherKey", values);
        RestResult CUT = new RestResult();
        CUT.addHeaderFrom(key, headers);
        assertEquals(firstValue, CUT.getFirstHeaderValue(key));
        assertEquals(1, CUT.getHeaders().size());
    }

    @Test
    public void toResponse() throws AddressException {
        String backendValue = "1.2.3.4";
        Status statusValue = Status.CREATED;
        User CUT = new User();
        CUT.setName("Fazil Ongudar");
        CUT.setEmail(new EmailAddress("fazil@ongudar.de"));
        CUT.addHeader(HttpHeaders.BACKEND, backendValue);
        CUT.setStatus(statusValue);
        Response response = CUT.toResponse();
        assertEquals(CUT, response.getEntity());
        assertEquals(backendValue, response.getHeaders().getFirst(HttpHeaders.BACKEND));
        assertEquals(statusValue.getStatusCode(), response.getStatus());
    }

    @Test
    public void getCookie_noCookie() {
        RestResult CUT = new RestResult();
        assertNull(CUT.getNewCookie());
    }

    @Test
    public void getCookie_nullCookie() {
        RestResult CUT = new RestResult();
        CUT.addHeader(HttpHeaders.SET_COOKIE, (String)null);
        assertNull(CUT.getNewCookie());
    }

    @Test
    public void setNewCookie_unmodifiableCollection() {
        Map<String, List<String>> randomHeaders = new HashMap<>();
        randomHeaders.put(HttpHeaders.SET_COOKIE, Arrays.asList(new String[] { "first", "second", "third" }));
        RestResult CUT = new RestResult();
        CUT.setNewCookie(randomHeaders);
        assertEquals("first", CUT.getNewCookie());
        CUT.getHeaders().get(HttpHeaders.SET_COOKIE).add("fourth");
        assertEquals(4, CUT.getHeaders().get(HttpHeaders.SET_COOKIE).size());
    }

    @Test
    public void getNewCookie() {
        RestResult CUT = new RestResult();
        CUT.addHeader(HttpHeaders.SET_COOKIE, "first", "second", "third");
        assertEquals("first", CUT.getNewCookie());
    }

}
