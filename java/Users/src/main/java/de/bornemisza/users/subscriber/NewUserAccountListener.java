package de.bornemisza.users.subscriber;

import java.util.logging.Logger;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import de.bornemisza.users.entity.User;

public class NewUserAccountListener implements MessageListener<User> {

    @Override
    public void onMessage(Message<User> msg) {
        Logger.getAnonymousLogger().info("New User detected: " + msg.getMessageObject().toString());
    }
    
}
