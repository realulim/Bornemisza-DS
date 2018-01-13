package de.bornemisza.rest.exception;

import javax.ejb.ApplicationException;

@ApplicationException
public class BusinessException extends RuntimeException {

    private final BusinessExceptionType type;

    public BusinessException(BusinessExceptionType type, String msg) {
        super(msg);
        this.type = type;
    }

    public BusinessExceptionType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "BusinessException{" + "type=" + type + "} " + super.toString();
    }

}
