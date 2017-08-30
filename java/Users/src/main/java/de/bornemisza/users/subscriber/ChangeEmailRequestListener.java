package de.bornemisza.users.subscriber;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import de.bornemisza.rest.entity.User;
import de.bornemisza.users.JAXRSConfiguration;
import de.bornemisza.users.MailSender;

@Singleton
@Startup
public class ChangeEmailRequestListener extends AbstractConfirmationMailListener {

    public ChangeEmailRequestListener() {
        super();
    }

    public ChangeEmailRequestListener(ITopic<User> aTopic, IMap<String, User> aMap, MailSender mailSender) {
        super(aTopic, aMap, mailSender);
    }

    @Override
    String getRequestTopicName() {
        return JAXRSConfiguration.TOPIC_CHANGE_EMAIL_REQUEST;
    }

    @Override
    String getRequestMapName() {
        return JAXRSConfiguration.MAP_CHANGE_EMAIL_REQUEST;
    }

    @Override
    String getMailSubject() {
        return "Confirmation of new E-Mail Address";
    }

    @Override
    String getHtmlTemplateFileName() {
        return "confirmation-mail-email.html";
    }

    @Override
    String getTextTemplateFileName() {
        return "confirmation-mail-email.txt";
    }

    @Override
    String getFallbackHtmlTemplate() {
        return "<html><h3>Please click on the confirmation link to change your e-mail address:</h3><a href=\"https://$FQDN$/users/confirmation/email/$UUID$\">Henceforth this shall be my E-Mail Address!</a></html>";
    }

    @Override
    String getFallbackTextTemplate() {
        return "Please copy this link into your browser to change your e-mail address: https://$FQDN$/users/confirmation/email/$UUID$";
    }
    
}
