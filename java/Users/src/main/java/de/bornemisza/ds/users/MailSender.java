package de.bornemisza.ds.users;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MailSender {

    @Resource(name = "mail/Outgoing")
    private Session mailSession;

    /**
     * @param recipient not null
     * @param subject not null
     * @param textContent not null
     * @param htmlContent null
     * @return whether the mail could be sent or not
     */
    public boolean sendMail(InternetAddress recipient, String subject, String textContent, String htmlContent) {
        try {
            MimeMessage msg = new MimeMessage(mailSession);
            msg.setSubject(subject);
            msg.setRecipient(Message.RecipientType.TO, recipient);

            if (htmlContent == null || htmlContent.isEmpty()) {
                // Text only Mail
                msg.setContent(textContent, "text/plain; charset=utf-8");
            }
            else {
                // HTML Mail with Text alternative
                final MimeMultipart multiPart = new MimeMultipart("alternative");

                final MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(textContent, "text/plain; charset=utf-8"); 
                multiPart.addBodyPart(textPart);

                final MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
                multiPart.addBodyPart(htmlPart);

                msg.setContent(multiPart);
            }

            Transport.send(msg);
            return true;
        }
        catch (MessagingException ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Cannot send Mail", ex);
            return false;
        }
    }

}
