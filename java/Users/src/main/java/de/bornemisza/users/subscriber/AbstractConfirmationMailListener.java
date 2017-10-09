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
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import de.bornemisza.couchdb.entity.User;
import de.bornemisza.users.MailSender;

public abstract class AbstractConfirmationMailListener implements MessageListener<User> {

    @Inject
    protected MailSender mailSender;

    @Inject
    protected HazelcastInstance hazelcast;

    abstract String getRequestTopicName();
    abstract String getRequestMapName();
    abstract String getMailSubject();
    abstract String getHtmlTemplateFileName();
    abstract String getTextTemplateFileName();
    abstract String getFallbackHtmlTemplate();
    abstract String getFallbackTextTemplate();

    private ITopic<User> requestTopic;
    private IMap<String, User> requestMap;
    private String registrationId;

    protected final String FQDN = System.getProperty("FQDN");

    public AbstractConfirmationMailListener() {
    }

    // Constructor for Unit Tests
    public AbstractConfirmationMailListener(ITopic<User> aTopic, IMap<String, User> aMap, MailSender mailSender) {
        this.requestTopic = aTopic;
        this.requestMap = aMap;
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        this.requestTopic = hazelcast.getTopic(getRequestTopicName());
        this.registrationId = requestTopic.addMessageListener(this);
        this.requestMap = hazelcast.getMap(getRequestMapName());
    }

    private InternetAddress createRecipient(User user) {
        try {
            return new InternetAddress(user.getEmail().getAddress(), user.getName());
        }
        catch (UnsupportedEncodingException ex) {
            return user.getEmail();
        }
    }

    @Override
    public void onMessage(Message<User> msg) {
        User user = msg.getMessageObject();
        Logger.getAnonymousLogger().info("Request detected on Topic " + getRequestTopicName() + " for: " + user.toString());
        String uuid = UUID.randomUUID().toString();
        User previousValue = this.requestMap.putIfAbsent(uuid, user);
        if (previousValue == null) {
            boolean mailSent = sendConfirmationMail(user, uuid);
            if (!mailSent) {
                this.requestMap.remove(uuid);
            }
        } else {
            Logger.getAnonymousLogger().warning("UUID clash: " + uuid);
        }
        Logger.getAnonymousLogger().info("Unconfirmed Requests in " + getRequestMapName() + ": " + requestMap.size());
    }

    private boolean sendConfirmationMail(User user, String uuid) {
        InternetAddress recipient = createRecipient(user);
        String textContent = createContent(user, uuid, getTextTemplateFileName(), getFallbackTextTemplate());
        String htmlContent = createContent(user, uuid, getHtmlTemplateFileName(), getFallbackHtmlTemplate());
        boolean success = mailSender.sendMail(recipient, getMailSubject(), textContent, htmlContent);
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
        requestTopic.removeMessageListener(registrationId);
        Logger.getAnonymousLogger().info("Message Listener " + registrationId + " removed.");
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

}
