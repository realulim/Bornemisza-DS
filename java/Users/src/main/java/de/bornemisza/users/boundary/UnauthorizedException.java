package de.bornemisza.users.boundary;

import javax.ejb.ApplicationException;

@ApplicationException
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String msg) {
        super(msg);
    }

}
