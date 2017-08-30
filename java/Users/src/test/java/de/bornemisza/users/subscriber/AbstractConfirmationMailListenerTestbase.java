package de.bornemisza.users.subscriber;

import java.io.IOException;

import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import static org.junit.Assert.assertTrue;

import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;

import de.bornemisza.rest.entity.User;
import de.bornemisza.users.MailSender;

public abstract class AbstractConfirmationMailListenerTestbase {
    
    private User user;
    private Message<User> msg;
    private ITopic<User> requestTopic;
    private IMap<String, User> requestMap;
    private MailSender mailSender;

    AbstractConfirmationMailListener CUT;

    abstract AbstractConfirmationMailListener getRequestListener(ITopic topic, IMap map, MailSender mailSender);
    abstract String getConfirmationLinkPrefix();

    @Before
    public void setUp() throws AddressException {
        user = new User();
        InternetAddress recipient = new InternetAddress("user@localhost.de");
        user.setName("Fazil Ongudar");
        user.setEmail(recipient);
        System.setProperty("FQDN", "www.bornemisza.de");

        msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(user);

        requestTopic = mock(ITopic.class);
        requestMap = mock(IMap.class);
        mailSender = mock(MailSender.class);
        CUT = getRequestListener(requestTopic, requestMap, mailSender);
    }

    protected void onMessage_mailSent_styledTemplate_Base() throws AddressException, NoSuchProviderException, IOException {
        ArgumentCaptor<String> uuidCaptor =  ArgumentCaptor.forClass(String.class);
        when(requestMap.putIfAbsent(uuidCaptor.capture(), eq(user))).thenReturn(null);

        ArgumentCaptor<String> textContentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlContentCaptor = ArgumentCaptor.forClass(String.class);
        when(mailSender.sendMail(any(InternetAddress.class), anyString(), textContentCaptor.capture(), htmlContentCaptor.capture())).thenReturn(true);

        CUT.onMessage(msg);

        // Text-Mailbody must contain Link
        String expectedLink = getConfirmationLinkPrefix() + "/" + uuidCaptor.getValue();
        String textMailBody = textContentCaptor.getValue();
        assertTrue(textMailBody.contains(expectedLink));

        // Html-Mailbody must contain Link and Style
        expectedLink = "<a href=\"" + getConfirmationLinkPrefix() + "/" + uuidCaptor.getValue() + "\"";
        String htmlMailBody = htmlContentCaptor.getValue();
        assertTrue(htmlMailBody.contains(expectedLink));
        assertTrue(htmlMailBody.contains("font-family:Helvetica, Arial, sans-serif"));
    }
    
    protected void onMessage_mailNotSent_Base() throws AddressException, NoSuchProviderException, IOException {
        ArgumentCaptor<String> uuidCaptor =  ArgumentCaptor.forClass(String.class);
        when(requestMap.putIfAbsent(uuidCaptor.capture(), eq(user))).thenReturn(null);

        when(mailSender.sendMail(eq(user.getEmail()), contains("Confirmation"), anyString(), anyString())).thenReturn(false);
        CUT.onMessage(msg);

        verify(requestMap).remove(uuidCaptor.getValue());
    }

    protected void onMessage_uuidClash_doNotSendAdditionalMail_Base() throws AddressException, NoSuchProviderException {
        when(requestMap.putIfAbsent(anyString(), eq(user))).thenReturn(user);

        CUT.onMessage(msg);

        verifyNoMoreInteractions(mailSender);
    }

}
