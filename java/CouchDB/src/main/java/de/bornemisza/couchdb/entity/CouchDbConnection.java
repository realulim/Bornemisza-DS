package de.bornemisza.couchdb.entity;

import java.net.URL;

public class CouchDbConnection {

    private final URL url;
    private final String databaseName;
    private final String userName;
    private final String password;

    public CouchDbConnection(URL url, String databaseName, String userName, String password) {
        this.url = url;
        this.databaseName = databaseName;
        this.userName = userName;
        this.password = password;
    }

    public URL getUrl() {
        return url;
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
        return "CouchDbConnection{" + "url=" + url + ", databaseName=" + databaseName + ", userName=" + userName + ", password=" + password + '}';
    }

}
