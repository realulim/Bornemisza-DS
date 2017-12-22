package de.bornemisza.rest.entity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class User extends Document implements Serializable {

    private static final long serialVersionUID = 1L;

    public static String USERNAME_PREFIX = "org.couchdb.user:"; // CouchDB wants this

    public User() {
        // JAXB needs this
        super();
        this.type = "user";
    }

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "password")
    private char[] password;

    @JsonProperty(value = "email")
    @JsonSerialize(using = ToStringSerializer.class)
    private EmailAddress email;

    @JsonProperty(value = "roles")
    private List<String> roles;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setId(USERNAME_PREFIX + name);
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public void setEmail(EmailAddress email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public static String db(String userName) {
        byte[] userNameBytes = userName.getBytes();
        String hexEncodedUserName = DatatypeConverter.printHexBinary(userNameBytes).toLowerCase();
        return "userdb-" + hexEncodedUserName;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.name);
        hash = 61 * hash + Arrays.hashCode(this.password);
        hash = 61 * hash + Objects.hashCode(this.email);
        hash = 61 * hash + Objects.hashCode(this.roles);
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
        final User other = (User) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Arrays.equals(this.password, other.password)) {
            return false;
        }
        if (!Objects.equals(this.email, other.email)) {
            return false;
        }
        if (!Objects.equals(this.roles, other.roles)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + " => " + "User{" + "name=" + name + ", password=" + password + ", email=" + email + ", roles=" + roles + "}";
    }

}
