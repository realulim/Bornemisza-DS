package de.bornemisza.sessions.boundary;

import javax.ejb.ApplicationException;

@ApplicationException
public class TechnicalException extends RuntimeException {

    public TechnicalException(String msg) {
        super(msg);
    }

}
