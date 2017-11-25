package de.bornemisza.rest.exception;

import javax.ejb.ApplicationException;

@ApplicationException
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String msg) {
        super(msg);
    }

}
