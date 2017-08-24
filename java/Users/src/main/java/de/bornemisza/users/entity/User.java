package de.bornemisza.users.entity;

import java.util.List;

import javax.mail.internet.InternetAddress;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import org.ektorp.support.CouchDbDocument;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends CouchDbDocument {

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

    @JsonProperty(value = "derived_key")
    private String derivedKey;

    @JsonProperty(value = "salt")
    private String salt;

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

    public void setName(@NotNull String name) {
        this.name = name;
        setId(USERNAME_PREFIX + name);
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(@NotNull char[] password) {
        this.password = password;
    }

    public InternetAddress getEmail() {
        return email;
    }

    public void setEmail(@NotNull InternetAddress email) {
        this.email = email;
    }

    public String getDerivedKey() {
        return derivedKey;
    }

    public void setDerivedKey(String derivedKey) {
        this.derivedKey = derivedKey;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(@NotNull List<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "CouchDbDocument{" + "id=" + getId() + ", rev=" + getRevision() + ", attachments=" + getAttachments() + ", conflicts=" + getConflicts() + "}" +
               "User{" + "type=" + type + ", name=" + name + ", password=" + password + ", email=" + email + ", derivedKey=" + derivedKey + ", salt=" + salt + ", roles=" + roles + '}';
    }

}
