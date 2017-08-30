package de.bornemisza.rest.entity;

import org.ektorp.support.CouchDbDocument;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Session extends CouchDbDocument {

    public Session() {
        // JAXB needs this
        super();
    }

    @JsonProperty(value = "type")
    private String type = "session";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
