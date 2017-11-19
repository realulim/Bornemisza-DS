package de.bornemisza.rest.entity;

import java.util.List;

import javax.mail.internet.InternetAddress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends Document {

    public static String USERNAME_PREFIX = "org.couchdb.user:"; // CouchDB wants this

    public User() {
        // JAXB needs this
        super();
    }

    @JsonProperty(value = "type")
    private String type = "user";

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "password")
    private char[] password;

    @JsonProperty(value = "email")
    @JsonSerialize(using = ToStringSerializer.class)
    private InternetAddress email;

    @JsonProperty(value = "roles")
    private List<String> roles;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    /**
     * @param name not null
     */
    public void setName(String name) {
        this.name = name;
        setId(USERNAME_PREFIX + name);
    }

    public char[] getPassword() {
        return password;
    }

    /**
     * @param password not null
     */
    public void setPassword(char[] password) {
        this.password = password;
    }

    public InternetAddress getEmail() {
        return email;
    }

    /**
     * @param email not null
     */
    public void setEmail(InternetAddress email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    /**
     * @param roles not null
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "CouchDbDocument{" + "id=" + getId() + ", rev=" + getRevision() + ", conflicts=" + getConflicts() + "}" +
               "User{" + "type=" + type + ", name=" + name + ", password=******" + ", email=" + email + ", roles=" + roles + '}';
    }

}
