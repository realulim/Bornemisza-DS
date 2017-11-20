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
        Get get = http.get("");
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

}
