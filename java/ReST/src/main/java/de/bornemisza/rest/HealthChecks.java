package de.bornemisza.rest;

import java.util.List;
import java.util.Map;
import org.javalite.http.Post;

public class HealthChecks {

    public boolean isCouchDbReady(Http http) {
        Post post = http.post("");
        Map<String, List<String>> headers = post.headers();
        List<String> header = headers.get("Server");
        return header != null && !header.isEmpty() && header.get(0).startsWith("CouchDB");
    }

}
