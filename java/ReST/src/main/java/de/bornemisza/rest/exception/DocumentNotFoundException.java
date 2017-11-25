package de.bornemisza.rest.exception;

import javax.ejb.ApplicationException;

@ApplicationException
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String msg) {
        super(msg);
    }

}
