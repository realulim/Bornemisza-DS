package de.bornemisza.users.subscriber;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.MailSender;
import de.bornemisza.users.entity.User;

@Singleton
@Startup
public class NewUserAccountListener implements MessageListener<User> {

    @Inject
    private MailSender mailSender;

    @Inject
    HazelcastInstance hazelcast;

    private ITopic<User> newUserAccountTopic;
    private IMap<String, User> newUserAccountMap;
    private String registrationId;

    private final String FQDN = System.getProperty("FQDN");

    public NewUserAccountListener() {
    }

    // Constructor for Unit Tests
    public NewUserAccountListener(ITopic<User> newUserAccountTopic, IMap<String, User> newUserAccountMap, MailSender mailSender) {
        this.newUserAccountTopic = newUserAccountTopic;
        this.newUserAccountMap = newUserAccountMap;
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        this.newUserAccountTopic = hazelcast.getTopic(JAXRSConfiguration.TOPIC_NEW_USER_ACCOUNT);
        this.registrationId = newUserAccountTopic.addMessageListener(this);
        this.newUserAccountMap = hazelcast.getMap(JAXRSConfiguration.MAP_NEW_USER_ACCOUNT);
    }

    @Override
    public void onMessage(Message<User> msg) {
        User user = msg.getMessageObject();
        Logger.getAnonymousLogger().info("Detected new User: " + user.toString());
        String uuid = UUID.randomUUID().toString();
        User previousValue = this.newUserAccountMap.putIfAbsent(uuid, user);
        if (previousValue == null) {
            boolean mailSent = sendConfirmationMail(user, uuid);
            if (!mailSent) {
                this.newUserAccountMap.remove(uuid);
            }
        } else {
            Logger.getAnonymousLogger().warning("UUID clash: " + uuid);
        }
        Logger.getAnonymousLogger().info("Unconfirmed users: " + newUserAccountMap.size());
    }

    private boolean sendConfirmationMail(User user, String uuid) {
        InternetAddress recipient = createRecipient(user);
        String subject = "Confirmation of new User Account";
        String textContent = createContent(user, uuid, "confirmation-mail-user.txt", fallbackTextTemplate);
        String htmlContent = createContent(user, uuid, "confirmation-mail-user.html", fallbackHtmlTemplate);
        boolean success = mailSender.sendMail(recipient, subject, textContent, htmlContent);
        if (success) {
            Logger.getAnonymousLogger().info("Sent " + uuid + " to " + recipient.getAddress());
        }
        else {
            Logger.getAnonymousLogger().info("Sending " + uuid + " to " + recipient.getAddress() + " failed!");
        }
        return success;
    }

    @PreDestroy
    private void dispose() {
        newUserAccountTopic.removeMessageListener(registrationId);
    }

    private InternetAddress createRecipient(User user) {
        try {
            return new InternetAddress(user.getEmail().getAddress(), user.getName());
        }
        catch (UnsupportedEncodingException ex) {
            return user.getEmail();
        }
    }

    private String createContent(User user, String uuid, String template, String fallbackTemplate) {
        String content;
        try {
            content = getResourceFile(template);
        }
        catch (IOException ioe) {
            content = fallbackTemplate;
        }
        return content.replace("$FQDN$", FQDN).replace("$NAME$", user.getName()).replace("$UUID$", uuid);
    }

    private String getResourceFile(String fileName) throws IOException {
        FileInputStream inputStream = new FileInputStream(getClass().getClassLoader().getResource(fileName).getFile());
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

    private final String fallbackHtmlTemplate = "<html><h3>Please click on the confirmation link to create your user account:</h3><a href=\"https://$FQDN$/users/confirmation/user/$UUID$\">Yes, I'm $NAME$ and I want to become a Member!</a></html>";
    private final String fallbackTextTemplate = "Dear $NAME$, please copy this link into your browser to create your user account: https://$FQDN$/users/confirmation/user/$UUID$";

}
