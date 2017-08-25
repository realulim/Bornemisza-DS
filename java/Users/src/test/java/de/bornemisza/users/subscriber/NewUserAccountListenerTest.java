package de.bornemisza.users.subscriber;

import java.io.IOException;

import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;

import de.bornemisza.users.MailSender;
import de.bornemisza.users.entity.User;

public class NewUserAccountListenerTest {

    private User user;
    private Message<User> msg;
    private ITopic<User> newUserAccountTopic;
    private IMap<String, User> newUserAccountMap;
    private MailSender mailSender;

    NewUserAccountListener CUT;

    public NewUserAccountListenerTest() {
    }

    @Before
    public void setUp() throws AddressException {
        user = new User();
        InternetAddress recipient = new InternetAddress("user@localhost.de");
        user.setName("Fazil Ongudar");
        user.setEmail(recipient);
        System.setProperty("FQDN", "www.bornemisza.de");

        msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(user);

        newUserAccountTopic = mock(ITopic.class);
        newUserAccountMap = mock(IMap.class);
        mailSender = mock(MailSender.class);
        CUT = new NewUserAccountListener(newUserAccountTopic, newUserAccountMap, mailSender);
    }

    @Test
    public void onMessage_mailSent_styledTemplate() throws AddressException, NoSuchProviderException, IOException {
        ArgumentCaptor<String> uuidCaptor =  ArgumentCaptor.forClass(String.class);
        when(newUserAccountMap.putIfAbsent(uuidCaptor.capture(), eq(user))).thenReturn(null);

        ArgumentCaptor<String> textContentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlContentCaptor = ArgumentCaptor.forClass(String.class);
        when(mailSender.sendMail(any(InternetAddress.class), anyString(), textContentCaptor.capture(), htmlContentCaptor.capture())).thenReturn(true);

        CUT.onMessage(msg);

        // Text-Mailbody must contain Link
        String expectedLink = "https://" + System.getProperty("FQDN") + "/users/confirmation/" + uuidCaptor.getValue();
        String textMailBody = textContentCaptor.getValue();
        System.out.println(textMailBody);
        assertTrue(textMailBody.contains(expectedLink));

        // Html-Mailbody must contain Link and Style
        expectedLink = "<a href=\"https://" + System.getProperty("FQDN") + "/users/confirmation/" + uuidCaptor.getValue() + "\"";
        String htmlMailBody = htmlContentCaptor.getValue();
        assertTrue(htmlMailBody.contains(expectedLink));
        assertTrue(htmlMailBody.contains("font-family:Helvetica, Arial, sans-serif"));
    }
    
    @Test
    public void onMessage_mailNotSent() throws AddressException, NoSuchProviderException, IOException {
        ArgumentCaptor<String> uuidCaptor =  ArgumentCaptor.forClass(String.class);
        when(newUserAccountMap.putIfAbsent(uuidCaptor.capture(), eq(user))).thenReturn(null);

        when(mailSender.sendMail(eq(user.getEmail()), contains("Confirmation"), anyString(), anyString())).thenReturn(false);
        CUT.onMessage(msg);

        verify(newUserAccountMap).remove(uuidCaptor.getValue());
    }

    @Test
    public void onMessage_uuidClash_doNotSendAdditionalMail() throws AddressException, NoSuchProviderException {
        when(newUserAccountMap.putIfAbsent(anyString(), eq(user))).thenReturn(user);

        CUT.onMessage(msg);

        verifyNoMoreInteractions(mailSender);
    }

}
