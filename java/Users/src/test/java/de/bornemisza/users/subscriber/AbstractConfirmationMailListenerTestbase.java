package de.bornemisza.users.subscriber;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import static org.junit.Assert.assertTrue;

import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;

import de.bornemisza.couchdb.entity.User;
import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.MailSender;

public abstract class AbstractConfirmationMailListenerTestbase {
    
    private User user;
    private Message<User> msg;
    private ITopic requestTopic;
    private IMap userIdMap, uuidMap;
    private MailSender mailSender;
    private HazelcastInstance hazelcast;

    AbstractConfirmationMailListener CUT;

    abstract AbstractConfirmationMailListener getRequestListener(MailSender mailSender, HazelcastInstance hz);
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
        userIdMap = mock(IMap.class);

        uuidMap = mock(IMap.class);
        mailSender = mock(MailSender.class);
        hazelcast = mock(HazelcastInstance.class);
        when(hazelcast.getMap(endsWith(JAXRSConfiguration.MAP_USERID_SUFFIX))).thenReturn(userIdMap);
        when(hazelcast.getMap(endsWith(JAXRSConfiguration.MAP_UUID_SUFFIX))).thenReturn(uuidMap);
        when(hazelcast.getTopic(anyString())).thenReturn(requestTopic);
        CUT = getRequestListener(mailSender, hazelcast);
    }

    protected void onMessage_mailSent_styledTemplate_Base() throws AddressException, NoSuchProviderException, IOException {
        ArgumentCaptor<String> uuidCaptor =  ArgumentCaptor.forClass(String.class);
        when(userIdMap.putIfAbsent(eq(user.getId()), eq("locked"), anyLong(), any(TimeUnit.class))).thenReturn(null);

        ArgumentCaptor<String> textContentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlContentCaptor = ArgumentCaptor.forClass(String.class);
        when(mailSender.sendMail(any(InternetAddress.class), anyString(), textContentCaptor.capture(), htmlContentCaptor.capture())).thenReturn(true);

        CUT.onMessage(msg);

        // Text-Mailbody must contain Link
        verify(uuidMap).set(uuidCaptor.capture(), any(User.class), anyLong(), any(TimeUnit.class));
        String expectedLink = getConfirmationLinkPrefix() + uuidCaptor.getValue();
        String textMailBody = textContentCaptor.getValue();
        assertTrue(textMailBody.contains(expectedLink));

        // Html-Mailbody must contain Link and Style
        expectedLink = "<a href=\"" + getConfirmationLinkPrefix() + uuidCaptor.getValue() + "\"";
        String htmlMailBody = htmlContentCaptor.getValue();
        assertTrue(htmlMailBody.contains(expectedLink));
        assertTrue(htmlMailBody.contains("font-family:Helvetica, Arial, sans-serif"));
    }
    
    protected void onMessage_mailNotSent_Base() throws AddressException, NoSuchProviderException, IOException {
        ArgumentCaptor<String> uuidCaptor =  ArgumentCaptor.forClass(String.class);
        when(userIdMap.putIfAbsent(eq(user.getId()), eq("locked"), anyLong(), any(TimeUnit.class))).thenReturn(null);
        when(uuidMap.put(uuidCaptor.capture(), any(User.class), anyLong(), any(TimeUnit.class))).thenReturn(null);

        when(mailSender.sendMail(eq(user.getEmail()), contains("Confirmation"), anyString(), anyString())).thenReturn(false);
        CUT.onMessage(msg);

        verify(userIdMap).delete(user.getId());
        verify(uuidMap).delete(anyString());
    }

    protected void onMessage_userExists_doNotSendAdditionalMail_Base() throws AddressException, NoSuchProviderException {
        User existingUser = new User();
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        when(userIdMap.putIfAbsent(eq(user.getId()), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(UUID.randomUUID().toString());
        when(uuidMap.values()).thenReturn(Arrays.asList(new User[] { existingUser }));
        CUT.onMessage(msg);

        verify(userIdMap).putIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(userIdMap);
        verify(uuidMap).values();
        verifyNoMoreInteractions(uuidMap);
        verifyNoMoreInteractions(mailSender);
    }

    protected void onMessage_uuidExists_doNotSendAdditionalMail_locked_Base() throws AddressException, NoSuchProviderException {
        when(userIdMap.putIfAbsent(eq(user.getId()), anyString(), anyLong(), any(TimeUnit.class))).thenReturn("locked");
        CUT.onMessage(msg);

        verifyNoMoreInteractions(mailSender);
        verify(userIdMap).putIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(userIdMap);
        verifyNoMoreInteractions(uuidMap);
    }

    protected void onMessage_uuidExists_sendAdditionalMail_Base() throws AddressException, NoSuchProviderException {
        String previousValue = UUID.randomUUID().toString();
        when(userIdMap.putIfAbsent(eq(user.getId()), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(previousValue);
        when(uuidMap.values()).thenReturn(Arrays.asList(new User[] { user }));
        User userWithDifferentMailAddress = new User();
        userWithDifferentMailAddress.setName(user.getName());
        userWithDifferentMailAddress.setPassword(user.getPassword());
        InternetAddress otherRecipient = new InternetAddress("user@otheraddress.de");
        userWithDifferentMailAddress.setEmail(otherRecipient);

        when(uuidMap.get(previousValue)).thenReturn(user);
        when(hazelcast.getMap(endsWith(JAXRSConfiguration.MAP_USERID_SUFFIX))).thenReturn(userIdMap);
        when(msg.getMessageObject()).thenReturn(userWithDifferentMailAddress);

        CUT = getRequestListener(mailSender, hazelcast);
        CUT.onMessage(msg);

        verify(uuidMap).set(anyString(), eq(userWithDifferentMailAddress), eq(24l), eq(TimeUnit.HOURS));
        verify(mailSender).sendMail(eq(otherRecipient), anyString(), anyString(), anyString());
        verifyNoMoreInteractions(mailSender);
    }

}
