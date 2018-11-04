package de.bornemisza.ds.users.subscriber;

import de.bornemisza.ds.users.subscriber.ChangeEmailRequestListener;
import de.bornemisza.ds.users.subscriber.AbstractConfirmationMailListener;
import java.io.IOException;

import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;

import org.junit.Test;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.ds.users.MailSender;

public class ChangeEmailRequestListenerTest extends AbstractConfirmationMailListenerTestbase {

    @Override
    AbstractConfirmationMailListener getRequestListener(MailSender mailSender, HazelcastInstance hz) {
        return new ChangeEmailRequestListener(mailSender, hz);
    }

    @Override
    String getConfirmationLinkPrefix() {
        return "https://" + System.getProperty("FQDN") + "/generic.html?action=confirm&type=email&uuid=";
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
    public void onMessage_userExists_doNotSendAdditionalMail() throws AddressException, NoSuchProviderException {
        onMessage_userExists_doNotSendAdditionalMail_Base();
    }

    @Test
    public void onMessage_uuidExists_doNotSendAdditionalMail_locked() throws AddressException, NoSuchProviderException {
        onMessage_uuidExists_doNotSendAdditionalMail_locked_Base();
    }

    @Test
    public void onMessage_uuidExists_sendAdditionalMailIfAddressDifferent() throws AddressException, NoSuchProviderException {
        onMessage_uuidExists_sendAdditionalMail_Base();
    }

}
