package de.bornemisza.users;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender {

    @Resource(name = "mail/Outgoing")
    private Session mailSession;

    public boolean sendMail(InternetAddress recipient, String subject, String content) {
        try {
            MimeMessage msg = new MimeMessage(mailSession);
            msg.setSubject(subject);
            msg.setRecipient(Message.RecipientType.TO, recipient);
            
            msg.setContent(content, "text/html; charset=utf-8");

            Transport.send(msg);
            return true;
        }
        catch (MessagingException ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Cannot send Mail", ex);
            return false;
        }
    }

}
