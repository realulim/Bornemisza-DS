package de.bornemisza.rest.entity.result;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class UuidsResult extends RestResult implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public UuidsResult() {
        // JAXB needs this
    }

    @JsonProperty(value = "uuids")
    private List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + Objects.hashCode(this.uuids);
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
        return Objects.equals(this.uuids, other.uuids);
    }

    @Override
    public String toString() {
        return "RestResult{" + "headers=" + headers + ", status=" + status + "}\n" +
               "UuidsResult{" + "uuids=" + uuids + "}";
    }

}
