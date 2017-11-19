package de.bornemisza.rest.entity;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailAddress extends InternetAddress {

    public EmailAddress() {
        // JAXB needs this
        super();
    }

    public EmailAddress(String address) throws AddressException {
        super(address);
    }

}
