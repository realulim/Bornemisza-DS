package de.bornemisza.rest.exception;

/**
 * implement this Interface with an enum to define your specific types
 */
public interface BusinessExceptionType {

    String name();
    int ordinal();
}
