package de.bornemisza.sessions.da;

import javax.annotation.Resource;

import de.bornemisza.couchdb.da.ConnectionPool;

public class SessionsService {
    
    @Resource(name="couchdb/Users")
    ConnectionPool pool;

    public SessionsService() {
    }

}
