package de.bornemisza.rest.entity.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class models a response from calling a ReST endpoint.
 */
public class RestResult implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public RestResult() {
        // JAXB needs this
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

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public void addHeader(String key, String... values) {
        List<String> headerValues = this.headers.get(key);
        if (headerValues == null) {
            headerValues = new ArrayList<>();
            headers.put(key, headerValues);
        }
        headerValues.addAll(Arrays.asList(values));
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
