package de.bornemisza.maintenance;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import de.bornemisza.couchdb.entity.CouchDbConnection;
import de.bornemisza.couchdb.entity.MyCouchDbConnector;
import de.bornemisza.loadbalancer.Config;

@Stateless
public class HealthCheckTask {
    
    @Resource
    private TimerService timerService;

    @Inject
    HazelcastInstance hazelcast;

    @Inject
    CouchAdminPool couchPool;

    private IMap<String, Integer> dbServerUtilisation;
    private static final Set<String> FAILING_HOSTS = new HashSet<>();

    public HealthCheckTask() {
    }

    // Constructor for Unit Tests
    public HealthCheckTask(HazelcastInstance hazelcast, CouchAdminPool couchPool) {
        this.hazelcast = hazelcast;
        this.couchPool = couchPool;
    }

    @PostConstruct
    public void init() {
        this.dbServerUtilisation = hazelcast.getMap(Config.UTILISATION);
    }

    public Timer createTimer(ScheduleExpression expression, TimerConfig timerConfig) {
        return timerService.createCalendarTimer(expression, timerConfig);
    }

    @Timeout
    public void healthCheckMaintenance() {
        Map<String, CouchDbConnection> connections = couchPool.getAllConnections();
        for (Map.Entry<String, Integer> entry : this.dbServerUtilisation.entrySet()) {
            String hostname = entry.getKey();
            CouchDbConnection conn = connections.get(hostname);
            if (conn == null) {
                // in case a new SRV record appeared
                MyCouchDbConnector connector = couchPool.getConnector();
                conn = connector.getCouchDbConnection();
                connections.put(hostname, conn);
            }
            if (isCouchDbReady(conn)) {
                if (FAILING_HOSTS.contains(hostname)) {
                    FAILING_HOSTS.remove(hostname);
                    resetUtilisation(); // start everyone on equal terms
                    this.dbServerUtilisation.put(hostname, 0);
                    Logger.getAnonymousLogger().info(hostname + " healthy again, putting it back in rotation");
                }
                // Host still healthy, just keep it in there
            }
            else {
                if (! FAILING_HOSTS.contains(hostname)) {
                    FAILING_HOSTS.add(hostname);
                    this.dbServerUtilisation.delete(hostname);
                    Logger.getAnonymousLogger().info(hostname + " failed, keeping it out of rotation");
                }
                // Host still unhealthy, don't bring it back yet
            }
        }
    }

    private void resetUtilisation() {
        for (String hostname : this.dbServerUtilisation.keySet()) {
            this.dbServerUtilisation.set(hostname, 0);
        }
    }

    private boolean isCouchDbReady(CouchDbConnection conn) {
        try {
            HttpClient httpClient = new StdHttpClient.Builder()
                        .url(conn.getBaseUrl())
                        .username(conn.getUserName())
                        .password(conn.getPassword())
                        .build();
            CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
            return dbInstance.checkIfDbExists(conn.getDatabaseName());
        }
        catch (DbAccessException e) {
            return false;
        }
    }

}
