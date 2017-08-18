package de.bornemisza.users.entity;

import java.util.UUID;
import javax.mail.internet.InternetAddress;

public class ConfirmationMail {

    private final InternetAddress recipient;    
    private final UUID uuid;

    public ConfirmationMail(InternetAddress recipient, UUID uuid) {
        this.recipient = recipient;
        this.uuid = uuid;
    }

    public InternetAddress getRecipient() {
        return recipient;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "ConfirmationMail{" + "recipient=" + recipient + ", uuid=" + uuid + '}';
    }

}
