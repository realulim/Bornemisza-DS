package de.bornemisza.ds.sessions.da;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.javalite.http.HttpException;
import org.javalite.http.Post;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.Json;
import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.ds.sessions.boundary.SessionsType;

public class SessionsService {
    
    @Inject
    CouchSessionsPool sessionsPool;

    @Resource(name="lbconfig/CouchUsersAsAdmin")
    LoadBalancerConfig lbConfig;

    DbAdminPasswordBasedHashProvider hashProvider;

    public SessionsService() {
    }

    @PostConstruct
    private void init() {
        this.hashProvider = new DbAdminPasswordBasedHashProvider(lbConfig);
    }

    // Constructor for Unit Tests
    public SessionsService(CouchSessionsPool sessionsPool, LoadBalancerConfig lbConfig) {
        this.sessionsPool = sessionsPool;
        this.lbConfig = lbConfig;
        init();
    }

    public Session createSession(Auth auth) throws BusinessException, TechnicalException, UnauthorizedException {
        Post post = sessionsPool.getConnection().getHttp().post("")
            .param("name", auth.getUsername())
            .param("password", auth.getPassword());
        try {
            int responseCode = post.responseCode();
            if (responseCode == 401) {
                throw new UnauthorizedException(post.responseMessage());
            }
            else if (responseCode != 200) {
                throw new BusinessException(SessionsType.UNEXPECTED, responseCode + ": " + post.responseMessage());
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
            String encodedJWT = hashProvider.encodeJasonWebToken(auth.getUsername());
            session.setDoubleSubmitToken(new DoubleSubmitToken(cookie, encodedJWT));
            return session;
        }
    }

}
