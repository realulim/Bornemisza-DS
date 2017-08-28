package de.bornemisza.users.subscriber;

import java.io.IOException;

import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;

import org.junit.Test;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.users.MailSender;

public class ChangeEmailRequestListenerTest extends AbstractConfirmationMailListenerTestbase {

    @Override
    AbstractConfirmationMailListener getRequestListener(ITopic topic, IMap map, MailSender mailSender) {
        return new ChangeEmailRequestListener(topic, map, mailSender);
    }

    @Override
    String getConfirmationLinkPrefix() {
        return "https://" + System.getProperty("FQDN") + "/users/confirmation/email";
    }

    @Test
    public void onMessage_mailSent_styledTemplate() throws AddressException, NoSuchProviderException, IOException {
        onMessage_mailSent_styledTemplate_Base();
    }

    @Test
    public void onMessage_mailNotSent() throws AddressException, NoSuchProviderException, IOException {
        onMessage_mailNotSent_Base();
    }

    @Test
    public void onMessage_uuidClash_doNotSendAdditionalMail() throws AddressException, NoSuchProviderException {
        onMessage_uuidClash_doNotSendAdditionalMail_Base();
    }

}
