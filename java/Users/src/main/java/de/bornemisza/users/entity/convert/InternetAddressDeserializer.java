package de.bornemisza.users.entity.convert;

import java.io.IOException;
import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class InternetAddressDeserializer extends JsonDeserializer<InternetAddress> {

    @Override
    public InternetAddress deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        String internetAddress = jp.getText().trim();
        try {
            return new InternetAddress(internetAddress);
        }
        catch (AddressException ex) {
            Logger.getAnonymousLogger().warning("Unable to deserialize: " + internetAddress);
            return null;
        }
    }
    
}
