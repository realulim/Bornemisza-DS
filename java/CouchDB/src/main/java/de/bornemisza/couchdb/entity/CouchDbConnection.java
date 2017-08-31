package de.bornemisza.couchdb.entity;

import java.net.URL;

public class CouchDbConnection {

    private final URL baseUrl;
    private final String databaseName;
    private final String userName;
    private final String password;

    public CouchDbConnection(URL baseUrl, String databaseName, String userName, String password) {
        this.baseUrl = baseUrl;
        this.databaseName = databaseName;
        this.userName = userName;
        this.password = password;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "CouchDbConnection{" + "baseUrl=" + baseUrl + ", databaseName=" + databaseName + ", userName=" + userName + ", password=" + password + '}';
    }

}
