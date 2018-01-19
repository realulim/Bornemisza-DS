package de.bornemisza.maintenance.task;

import javax.ejb.Stateless;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;

import de.bornemisza.rest.HttpConnection;

@Stateless
public class HealthChecks {

    public boolean isCouchDbReady(HttpConnection conn) {
        Http http = conn.getHttp();
        Get get = http.get("", 200, 1000);
        try {
            if (get.responseCode() != 200) {
                return false;
            }
        }
        catch (HttpException ex) {
            return false;
        }
        String expected = "{\"db_name\":\"" + conn.getDatabaseName() + "\"";
        return get.text().startsWith(expected);
    }

    public String getCouchDbStatus(HttpConnection conn) {
        Http http = conn.getHttp();
        Get get = http.get("");
        try {
            int responseCode = get.responseCode();
            if (responseCode != 200) {
                return "Response Code: " + responseCode;
            }
        }
        catch (HttpException ex) {
            Throwable t = ex.getCause();
            return t == null ? ex.toString() : t.toString();
        }
        String expected = "{\"db_name\":\"" + conn.getDatabaseName() + "\"";
        String body = get.text();
        if (body.startsWith(expected)) {
            return "OK";
        }
        else return body;
    }

}
