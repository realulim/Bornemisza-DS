package de.bornemisza.rest;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.javalite.http.Post;

public class HealthChecks {

    public boolean isCouchDbReady(Http http) {
        try {
            Post post = http.post("");
            Map<String, List<String>> headers = post.headers();
            List<String> header = headers.get("Server");
            if (header == null || header.isEmpty()) {
                Logger.getAnonymousLogger().warning("Health Check failed: No Header");
                return false;
            }
            else if (header.get(0).startsWith("CouchDB")) return true;
            else {
                Logger.getAnonymousLogger("Health Check failed: " + header.get(0));
                return false;
            }
        }
        catch (NullPointerException ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Stracktace", ex);
            return false;
        }
        catch (Exception e) {
            Logger.getAnonymousLogger().warning("CouchDB not ready: " + e.toString());
            return false;
        }
    }

}
