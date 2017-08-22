package de.bornemisza.users.subscriber;

import java.util.UUID;
import java.util.logging.Logger;

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

import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    @Inject
    @ConfigProperty(name="FQDN", defaultValue="UNKNOWN")
    private String FQDN;

    private ITopic<User> newUserAccountTopic;
    private IMap<String, User> newUserAccountMap;
    private String registrationId;

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
            String link = "https://" + FQDN + "/users/confirmation/" + uuid;
            boolean mailSent = sendConfirmationMail(user.getEmail(), link);
            if (!mailSent) this.newUserAccountMap.remove(uuid);
        }
        else {
            Logger.getAnonymousLogger().warning("UUID clash: " + uuid);
        }
        Logger.getAnonymousLogger().info("Unconfirmed users: " + newUserAccountMap.size());
    }

    private boolean sendConfirmationMail(InternetAddress recipient, String link) {
        String subject = "Confirmation of new User Account";
        String content = "<html><h3>Please click on the confirmation link to create your user account:</h3><a href=\"" + link + "\">I'm for real!</a></html>";
        boolean success = mailSender.sendMail(recipient, subject, content);
        if (success) Logger.getAnonymousLogger().info("Sent " + link + " to " + recipient.getAddress());
        else Logger.getAnonymousLogger().info("Sending " + link + " to " + recipient.getAddress() + " failed!");
        return success;
    }

    @PreDestroy
    private void dispose() {
        newUserAccountTopic.removeMessageListener(registrationId);
    }

}

