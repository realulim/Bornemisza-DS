package de.bornemisza.rest.entity.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.bornemisza.rest.HttpHeaders;

/**
 * This class models a response from calling a ReST endpoint.
 */
public class RestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public RestResult() {
        // JAXB needs this
    }

    public RestResult(Map<String, List<String>> headers) {
        this.headers.putAll(headers);
    }

    @JsonIgnore
    protected Map<String, List<String>> headers = new HashMap<>();

    @JsonIgnore
    protected Status status;

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getFirstHeaderValue(String key) {
        List<String> headerValues = this.headers.get(key);
        return headerValues == null || headerValues.isEmpty() ? null : headerValues.get(0);
    }

    public void setNewCookie(Map<String, List<String>> randomHeaders) {
        if (randomHeaders != null && !randomHeaders.isEmpty()) {
            List<String> cookies = randomHeaders.get(HttpHeaders.SET_COOKIE);
            if (cookies != null) addHeader(HttpHeaders.SET_COOKIE, cookies.get(0));
        }
    }

    public String getNewCookie() {
        if (this.headers.isEmpty()) return null;
        List<String> cookies = this.headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) return null;
        else return cookies.get(0);
    }

    public void addHeader(String key, String... values) {
        List<String> headerValues = this.headers.get(key);
        if (headerValues == null) {
            headerValues = new ArrayList<>();
            this.headers.put(key, headerValues);
        }
        headerValues.addAll(Arrays.asList(values));
    }

    public void addHeaderFrom(String key, Map<String, List<String>> randomHeaders) {
        List<String> headerValues = randomHeaders.get(key);
        if (headerValues != null) {
            this.headers.put(key, new ArrayList(headerValues));
        }
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Response toResponse() {
        ResponseBuilder response = Response.status(status);
        response.entity(this);
        for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                response.header(entry.getKey(), value);
            }
        }
        return response.build();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.headers);
        hash = 37 * hash + Objects.hashCode(this.status);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RestResult other = (RestResult) obj;
        if (!Objects.equals(this.headers, other.headers)) {
            return false;
        }
        if (this.status != other.status) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "RestResult{" + "headers=" + headers + ", status=" + status + '}';
    }

}
