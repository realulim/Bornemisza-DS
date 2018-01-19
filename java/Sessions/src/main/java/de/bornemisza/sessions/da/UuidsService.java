package de.bornemisza.sessions.da;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.javalite.http.Get;
import org.javalite.http.HttpException;
import org.javalite.http.Post;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.Json;
import de.bornemisza.rest.entity.Uuid;
import de.bornemisza.rest.entity.result.KeyValueViewResult;
import de.bornemisza.rest.entity.result.RestResult;
import de.bornemisza.rest.entity.result.UuidsResult;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.sessions.boundary.SessionsType;

public class UuidsService {

    @Inject
    CouchPool couchPool;

    @Resource(name="lbconfig/CouchUsersAsAdmin")
    LoadBalancerConfig lbConfig;

    DbAdminPasswordBasedHashProvider hashProvider;

    public UuidsService() {
    }

    // Constructor for Unit Tests
    public UuidsService(CouchPool couchPool, LoadBalancerConfig lbConfig) {
        this.couchPool = couchPool;
        this.lbConfig = lbConfig;
        init();
    }

    @PostConstruct
    private void init() {
        this.hashProvider = new DbAdminPasswordBasedHashProvider(lbConfig);
    }

    public UuidsResult getUuids(int count) throws BusinessException, TechnicalException {
        Get get = couchPool.getConnection().getHttp().get("_uuids?count=" + count, 100, 1000);
        try {
            int responseCode = get.responseCode();
            if (responseCode != 200) {
                throw new BusinessException(SessionsType.GETUUIDS, responseCode + ": " + get.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        Map<String, List<String>> headers = get.headers();
        UuidsResult result = Json.fromJson(get.text(), UuidsResult.class);
        result.addHeaderFrom(HttpHeaders.BACKEND, headers);
        result.setNewCookie(headers);
        return result;
    }

    public RestResult saveUuids(Auth auth, String userDatabase, Uuid uuidDocument) throws UnauthorizedException, BusinessException, TechnicalException {
        Post post = couchPool.getConnection().getHttp().post(userDatabase, Json.toJson(uuidDocument))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, auth.getCookie());
        try {
            int responseCode = post.responseCode();
            if (responseCode == 401) {
                throw new UnauthorizedException(post.responseMessage());
            }
            else if (responseCode < 201 || responseCode > 202) {
                throw new BusinessException(SessionsType.SAVEUUIDS, responseCode + ": " + post.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        return new RestResult(post.headers());
    }

    public KeyValueViewResult loadColors(Auth auth, String userDatabase) throws BusinessException, TechnicalException {
        Get get = couchPool.getConnection().getHttp().get(userDatabase + "/_design/Uuid/_view/uuid_sum_by_color?group=true", 5000, 30000)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.COOKIE, auth.getCookie());
        try {
            int responseCode = get.responseCode();
            if (responseCode == 401) {
                throw new UnauthorizedException(get.responseMessage());
            }
            else if (responseCode != 200) {
                throw new BusinessException(SessionsType.LOADCOLORS, responseCode + ": " + get.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        KeyValueViewResult result = Json.fromJson(get.text(), KeyValueViewResult.class);
        result.setNewCookie(get.headers());
        return result;
    }

}
