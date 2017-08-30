package de.bornemisza.couchdb.entity;

import java.net.URL;
import java.util.Objects;

public class CouchDbConnection {

    private URL url;
    private String databaseName;
    private String userName;
    private String password;

    public CouchDbConnection(URL url, String databaseName, String userName, String password) {
        this.url = url;
        this.databaseName = databaseName;
        this.userName = userName;
        this.password = password;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.url);
        hash = 47 * hash + Objects.hashCode(this.databaseName);
        hash = 47 * hash + Objects.hashCode(this.userName);
        hash = 47 * hash + Objects.hashCode(this.password);
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
        final CouchDbConnection other = (CouchDbConnection) obj;
        if (!Objects.equals(this.databaseName, other.databaseName)) {
            return false;
        }
        if (!Objects.equals(this.userName, other.userName)) {
            return false;
        }
        if (!Objects.equals(this.password, other.password)) {
            return false;
        }
        return Objects.equals(this.url, other.url);
    }

    @Override
    public String toString() {
        return "CouchDbConnection{" + "url=" + url + ", databaseName=" + databaseName + ", userName=" + userName + ", password=" + password + '}';
    }

}
