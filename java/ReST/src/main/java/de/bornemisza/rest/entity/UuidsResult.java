package de.bornemisza.rest.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class UuidsResult implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public UuidsResult() {
        // JAXB needs this
    }

    @JsonProperty(value = "uuids")
    private List<String> uuids;

    @JsonIgnore
    private String backendHeader;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public String getBackendHeader() {
        return backendHeader;
    }

    public void setBackendHeader(String backendHeader) {
        this.backendHeader = backendHeader;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.uuids);
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
        final UuidsResult other = (UuidsResult) obj;
        if (!Objects.equals(this.uuids, other.uuids)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UuidsResult{" + "uuids=" + uuids + '}';
    }

}
