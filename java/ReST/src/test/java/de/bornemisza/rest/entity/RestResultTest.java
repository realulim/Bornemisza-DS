package de.bornemisza.rest.entity;

import de.bornemisza.rest.entity.result.RestResult;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import de.bornemisza.rest.HttpHeaders;

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

}
