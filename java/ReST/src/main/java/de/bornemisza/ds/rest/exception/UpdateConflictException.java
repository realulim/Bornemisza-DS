package de.bornemisza.ds.rest.exception;

import javax.ejb.ApplicationException;

@ApplicationException
public class UpdateConflictException extends RuntimeException {

    public UpdateConflictException(String msg) {
        super(msg);
    }

}
