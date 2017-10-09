package de.bornemisza.users.subscriber;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.couchdb.entity.User;
import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.MailSender;

@Singleton
@Startup
public class NewUserAccountListener extends AbstractConfirmationMailListener {

    public NewUserAccountListener() {
        super();
    }

    // Constructor for Unit Tests
    public NewUserAccountListener(ITopic<User> aTopic, IMap<String, User> aMap, MailSender mailSender) {
        super(aTopic, aMap, mailSender);
    }

    @Override
    String getRequestTopicName() {
        return JAXRSConfiguration.TOPIC_NEW_USER_ACCOUNT;
    }

    @Override
    String getRequestMapName() {
        return JAXRSConfiguration.MAP_NEW_USER_ACCOUNT;
    }

    @Override
    String getMailSubject() {
        return "Confirmation of new User Account";
    }

    @Override
    String getHtmlTemplateFileName() {
        return "confirmation-mail-user.html";
    }

    @Override
    String getTextTemplateFileName() {
        return "confirmation-mail-user.txt";
    }

    @Override
    String getFallbackHtmlTemplate() {
        return "<html><h3>Please click on the confirmation link to create your user account:</h3><a href=\"https://$FQDN$/users/confirmation/user/$UUID$\">Yes, I'm $NAME$ and I want to become a Member!</a></html>";
    }

    @Override
    String getFallbackTextTemplate() {
        return "Dear $NAME$, please copy this link into your browser to create your user account: https://$FQDN$/users/confirmation/user/$UUID$";
    }

}
