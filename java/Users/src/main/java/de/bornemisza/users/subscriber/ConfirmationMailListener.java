package de.bornemisza.users.subscriber;

import java.util.logging.Logger;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import de.bornemisza.users.entity.ConfirmationMail;

public class ConfirmationMailListener implements MessageListener<ConfirmationMail> {

    @Override
    public void onMessage(Message<ConfirmationMail> msg) {
        Logger.getAnonymousLogger().info("Confirmation Mail detected: " + msg.getMessageObject().toString());
    }
    
}
