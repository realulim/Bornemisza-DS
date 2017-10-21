package de.bornemisza.users.subscriber;

import java.io.IOException;

import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;

import org.junit.Test;

import com.hazelcast.core.HazelcastInstance;

import de.bornemisza.users.MailSender;

public class NewUserAccountListenerTest extends AbstractConfirmationMailListenerTestbase {

    @Override
    AbstractConfirmationMailListener getRequestListener(MailSender mailSender, HazelcastInstance hz) {
        return new NewUserAccountListener(mailSender, hz);
    }

    @Override
    String getConfirmationLinkPrefix() {
        return "https://" + System.getProperty("FQDN") + "/confirmUser";
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
    public void onMessage_uuidExists_doNotSendAdditionalMail() throws AddressException, NoSuchProviderException {
        onMessage_uuidExists_doNotSendAdditionalMail_Base();
    }

    @Test
    public void onMessage_uuidExists_sendAdditionalMailIfAddressDifferent() throws AddressException, NoSuchProviderException {
        onMessage_uuidExists_sendAdditionalMail_Base();
    }

}
