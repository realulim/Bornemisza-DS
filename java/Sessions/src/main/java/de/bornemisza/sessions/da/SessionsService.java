package de.bornemisza.sessions.da;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.javalite.http.HttpException;
import org.javalite.http.Post;

import de.bornemisza.rest.Json;
import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.sessions.boundary.BusinessException;
import de.bornemisza.sessions.boundary.BusinessException.Type;
import de.bornemisza.sessions.security.DbAdminPasswordBasedHashProvider;

public class SessionsService {
    
    @Inject
    CouchSessionsPool sessionsPool;

    @Inject
    DbAdminPasswordBasedHashProvider hashProvider;

    public SessionsService() {
    }

    // Constructor for Unit Tests
    public SessionsService(CouchSessionsPool sessionsPool, DbAdminPasswordBasedHashProvider hashProvider) {
        this.sessionsPool = sessionsPool;
        this.hashProvider = hashProvider;
    }

    public Session createSession(BasicAuthCredentials creds) {
        Post post = sessionsPool.getConnection().getHttp().post("")
            .param("name", creds.getUserName())
            .param("password", creds.getPassword());
        try {
            int responseCode = post.responseCode();
            if (responseCode != 200) {
                throw new BusinessException(Type.UNEXPECTED, responseCode + ": " + post.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        Map<String, List<String>> headers = post.headers();
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        else {
            Session session = Json.fromJson(post.text(), Session.class);
            String cookie = cookies.get(0);
            String hmac = hashProvider.hmacDigest(cookie.substring(0, cookie.indexOf(";")));
            DoubleSubmitToken dsToken = new DoubleSubmitToken(cookie, hmac);
            session.setDoubleSubmitToken(dsToken);
            return session;
        }
    }

}
