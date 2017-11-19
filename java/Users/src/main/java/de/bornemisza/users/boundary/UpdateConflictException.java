package de.bornemisza.users.boundary;

import javax.ejb.ApplicationException;

@ApplicationException
public class UpdateConflictException extends RuntimeException {

    public UpdateConflictException(String msg) {
        super(msg);
    }

}
