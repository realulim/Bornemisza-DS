package de.bornemisza.rest;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.javalite.http.Get;
import org.javalite.http.HttpException;

public class HttpMulti {

    private final TreeMap<String, Http> connections = new TreeMap<>();
    private final Map<String, Integer> dbServerUtilisation;

    public HttpMulti(Map<String, Integer> dbServerUtilisation) {
        this.dbServerUtilisation = dbServerUtilisation;
    }

    public void add(String hostname, Http connection) {
        this.connections.put(hostname, connection);
    }

    public Get get(String url) {
        for(Map.Entry<String, Http> entry : connections.entrySet()) {
            try {
                trackUtilisation(entry.getKey());
                return entry.getValue().get(url);
            }
            catch (HttpException e) {
                Logger.getAnonymousLogger().info(entry.getValue().getBaseUrl() + " unreachable...");
            }
        }
        Logger.getAnonymousLogger().warning("No Database Backends available at all!");
        throw new RuntimeException("No Database Backend ready!");
    }

    void trackUtilisation(String hostname) {
        this.dbServerUtilisation.computeIfPresent(hostname, (k, v) -> v+1);
    }

}
