package de.bornemisza.sessions.boundary;

import javax.ejb.ApplicationException;

@ApplicationException
public class BusinessException extends RuntimeException {

    private final Type type;

    public enum Type { UNEXPECTED };

    public BusinessException(Type type, String msg) {
        super(msg);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

}
