package de.bornemisza.users.subscriber;



import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

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
        user.setEmail(recipient);

        msg = mock(Message.class);
        when(msg.getMessageObject()).thenReturn(user);

        newUserAccountTopic = mock(ITopic.class);
        newUserAccountMap = mock(IMap.class);
        mailSender = mock(MailSender.class);
        CUT = new NewUserAccountListener(newUserAccountTopic, newUserAccountMap, mailSender);
    }
    
    @Test
    public void onMessage_mailSent() throws AddressException, NoSuchProviderException {
        ArgumentCaptor<String> uuidCaptor =  ArgumentCaptor.forClass(String.class);
        when(newUserAccountMap.putIfAbsent(uuidCaptor.capture(), eq(user))).thenReturn(null);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        when(mailSender.sendMail(eq(user.getEmail()), contains("Confirmation"), contentCaptor.capture())).thenReturn(true);
        CUT.onMessage(msg);

        // MailBody must contain Link and generated UUID
        String mailBody = contentCaptor.getValue();
        assertTrue(mailBody.contains("https://"));
        assertTrue(mailBody.contains(uuidCaptor.getValue()));
    }

    @Test
    public void onMessage_mailNotSent() throws AddressException, NoSuchProviderException {
        ArgumentCaptor<String> uuidCaptor =  ArgumentCaptor.forClass(String.class);
        when(newUserAccountMap.putIfAbsent(uuidCaptor.capture(), eq(user))).thenReturn(null);

        when(mailSender.sendMail(eq(user.getEmail()), contains("Confirmation"), anyString())).thenReturn(false);
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
