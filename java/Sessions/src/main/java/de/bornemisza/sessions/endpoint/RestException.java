package de.bornemisza.sessions.endpoint;

import javax.ejb.ApplicationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@ApplicationException
public class RestException extends WebApplicationException {

    public RestException(Response response) {
        super(response);
    }

    public RestException(Response.Status status) {
        super(status);
    }

}
