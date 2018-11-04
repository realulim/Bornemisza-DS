package de.bornemisza.ds.sessions.consumer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;

import de.bornemisza.ds.sessions.JAXRSConfiguration;
import de.bornemisza.ds.sessions.da.UuidsService;

@Singleton
@Startup
public class UuidWriteListener implements ItemListener {

    @Inject
    protected HazelcastInstance hazelcast;

    @Inject
    UuidsService uuidsService;

    private IQueue<StoreUuidRequest> queue;

    public UuidWriteListener() {
    }

    // Constructor for Unit Tests
    public UuidWriteListener(HazelcastInstance hz, UuidsService uuidsService) {
        this.hazelcast = hz;
        this.uuidsService = uuidsService;
        init();
    }

    @PostConstruct
    private void init() {
        this.queue = hazelcast.getQueue(JAXRSConfiguration.QUEUE_UUID_STORE);
        this.queue.addItemListener(this, false);
    }

    @Override
    public void itemAdded(ItemEvent ie) {
        try {
            // try to take an item from the queue, if available
            StoreUuidRequest request = queue.poll(5, TimeUnit.SECONDS);
            if (request != null) {
                uuidsService.saveUuids(request.getAuth(), request.getDatabaseName(), request.getUuidDocument());
            }
        }
        catch (InterruptedException ex) {
            Logger.getAnonymousLogger().warning("While waiting for Queue: " + ex.toString());
        }
    }

    @Override
    public void itemRemoved(ItemEvent ie) {
        // ignore
    }

}
