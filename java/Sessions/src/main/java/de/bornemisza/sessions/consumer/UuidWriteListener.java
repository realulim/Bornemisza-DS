package de.bornemisza.sessions.consumer;

import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;

@Singleton
@Startup
public class UuidWriteListener implements ItemListener {

    @Override
    public void itemAdded(ItemEvent ie) {
        Logger.getAnonymousLogger().info("Item added: " + ie.toString());
    }

    @Override
    public void itemRemoved(ItemEvent ie) {
        Logger.getAnonymousLogger().info("Item removed: " + ie.toString());
    }
    
}
