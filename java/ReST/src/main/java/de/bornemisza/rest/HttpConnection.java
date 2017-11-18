package de.bornemisza.rest;

public class HttpConnection {

    private final String hostname;
    private final Http http;

    public HttpConnection(String hostname, Http http) {
        this.hostname = hostname;
        this.http = http;
    }

    public String getHostname() {
        return hostname;
    }

    public Http getHttp() {
        return http;
    }

}
