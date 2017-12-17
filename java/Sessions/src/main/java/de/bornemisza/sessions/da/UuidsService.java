package de.bornemisza.sessions.da;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.javalite.http.Get;
import org.javalite.http.HttpException;
import org.javalite.http.Put;

import de.bornemisza.loadbalancer.LoadBalancerConfig;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.Json;
import de.bornemisza.rest.entity.Uuid;
import de.bornemisza.rest.entity.UuidsResult;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.DbAdminPasswordBasedHashProvider;
import de.bornemisza.sessions.boundary.SessionsType;

public class UuidsService {

    @Inject
    CouchPool couchPool;

    @Resource(name="lbconfig/CouchUsers")
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
        Get get = couchPool.getConnection().getHttp().get("_uuids?count=" + count);
        try {
            int responseCode = get.responseCode();
            if (responseCode != 200) {
                throw new BusinessException(SessionsType.UNEXPECTED, responseCode + ": " + get.responseMessage());
            }
        }
        catch (HttpException ex) {
            throw new TechnicalException(ex.toString());
        }
        String header = "127.0.0.1";
        List<String> backendHeaders = get.headers().get("X-Backend");
        if (backendHeaders != null) header = backendHeaders.get(0);
        UuidsResult result = Json.fromJson(get.text(), UuidsResult.class);
        result.setBackendHeader(header);
        return result;
    }

    public void saveUuids(Auth auth, String userDatabase, List<Uuid> uuids) {
        for (Uuid uuid : uuids) {
            String json = Json.toJson(uuid);
            Put put = couchPool.getConnection().getHttp().put(userDatabase, json)
                    .header(HttpHeaders.COOKIE, auth.getCookie());
            try {
                int responseCode = put.responseCode();
                if (responseCode != 201) {
                    throw new BusinessException(SessionsType.UNEXPECTED, responseCode + ": " + put.responseMessage());
                }
            }
            catch (HttpException ex) {
                throw new TechnicalException(ex.toString());
            }
        }
    }

}
