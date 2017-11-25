package de.bornemisza.users.boundary;

import de.bornemisza.rest.exception.BusinessExceptionType;

public enum UsersType implements BusinessExceptionType {
    UNEXPECTED, UUID_NOT_FOUND, USER_NOT_FOUND, USER_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS
}
