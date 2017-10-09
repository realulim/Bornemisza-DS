package de.bornemisza.couchdb.entity;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Session {

    public Session() {
        // JAXB needs this
        super();
    }

    @JsonProperty(value = "type")
    private String type = "session";

    @JsonProperty(value = "ctoken")
    private String ctoken;

    @JsonProperty(value = "name")
    private String principal;

    @JsonProperty(value = "roles")
    private final List<String> roles = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCToken() {
        return ctoken;
    }

    public void setCToken(String cToken) {
        this.ctoken = cToken;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(@NotNull String name) {
        this.principal = name;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void addRole(@NotNull String role) {
        this.roles.add(role);
    }

}
