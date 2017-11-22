package de.bornemisza.rest;

import java.util.Arrays;

import javax.mail.internet.AddressException;

import static org.junit.Assert.*;
import org.junit.Test;

import de.bornemisza.rest.entity.Database;
import de.bornemisza.rest.entity.Document;
import de.bornemisza.rest.entity.EmailAddress;
import de.bornemisza.rest.entity.KeyValueViewResult;
import de.bornemisza.rest.entity.Session;
import de.bornemisza.rest.entity.User;

public class JsonTest {
    
    public JsonTest() {
    }

    @Test
    public void checkUser() throws AddressException {
        User user = new User();
        user.setEmail(new EmailAddress("user@host.com"));
        user.setName("Karl Hop");
        user.setPassword("mySecretPassword".toCharArray());
        assertMarshallAndUnmarshall(user);
    }

    @Test
    public void checkSession() {
        Session session = new Session();
        assertMarshallAndUnmarshall(session);
    }

    @Test
    public void checkDatabase() {
        Database db = new Database();
        assertMarshallAndUnmarshall(db);
    }

    @Test
    public void checkDocument() {
        Document doc = new Document();
        doc.setId("someId");
        doc.setRevision("someRevision");
        doc.setConflicts(Arrays.asList(new String[] { "confl1", "confl2" }));
        assertMarshallAndUnmarshall(doc);
    }

    @Test
    public void checkKeyValueResult() {
        KeyValueViewResult res = new KeyValueViewResult();
        assertMarshallAndUnmarshall(res);
    }

    private void assertMarshallAndUnmarshall(Object obj) {
        String json = Json.toJson(obj);
        Object fromJson = Json.fromJson(json, obj.getClass());
        assertEquals(obj, fromJson);
        assertEquals(json, Json.toJson(fromJson));
    }

}
