package de.bornemisza.ds.sessions.boundary;

import de.bornemisza.rest.exception.BusinessExceptionType;

public enum SessionsType implements BusinessExceptionType {

    UNEXPECTED, GETUUIDS, SAVEUUIDS, LOADCOLORS

}
