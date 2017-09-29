package de.bornemisza.rest;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.javalite.http.Post;

public class HealthChecks {

    public boolean isCouchDbReady(Http http) {
        try {
            Post post = http.post("");
            Map<String, List<String>> headers = post.headers();
            List<String> header = headers.get("Server");
            return header != null && !header.isEmpty() && header.get(0).startsWith("CouchDB");
        }
        catch (Exception e) {
            Logger.getAnonymousLogger().warning("CouchDB not ready: " + e.toString());
            return false;
        }
    }

}
