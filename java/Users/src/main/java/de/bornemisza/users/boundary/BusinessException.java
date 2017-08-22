package de.bornemisza.users.boundary;

import javax.ejb.ApplicationException;

@ApplicationException
public class BusinessException extends RuntimeException {

    private final Type type;

    public enum Type { UUID_NOT_FOUND };

    public BusinessException(Type type, String msg) {
        super(msg);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

}
