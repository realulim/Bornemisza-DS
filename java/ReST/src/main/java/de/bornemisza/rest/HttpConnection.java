package de.bornemisza.rest;

public class HttpConnection {

    private final String databaseName;
    private final Http http;

    public HttpConnection(String dbName, Http http) {
        this.databaseName = dbName;
        this.http = http;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public Http getHttp() {
        return http;
    }

}