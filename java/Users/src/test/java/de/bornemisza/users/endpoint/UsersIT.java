package de.bornemisza.users.endpoint;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import de.bornemisza.users.IntegrationTestBase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UsersIT extends IntegrationTestBase {

    private static String revision, derivedKey, salt;

    @Test
    public void t0_addUser() {
        deleteMails(user.getEmail());
        Response response = postUser(user, 202);
        assertEquals(0, response.getBody().prettyPrint().length());
    }

    @Test
    public void t1_confirmUser() {
        long start = System.currentTimeMillis();
        String confirmationLink = retrieveConfirmationLink(user.getEmail());
        assertTrue(confirmationLink.startsWith("https://"));
        System.out.println("Mail delivered after " + (System.currentTimeMillis() - start) + " ms.");
        Response response = clickConfirmationLink(confirmationLink);
        JsonPath jsonPath = response.jsonPath();
        assertEquals(user.getEmail().toString(), jsonPath.getString("email"));
        assertEquals("******", jsonPath.getString("password"));
    }

    @Test
    public void t2_readUser_notAuthorized() {
        getUser(user.getName(), 401);
    }

    @Test
    public void t3_readUser_unauthorized() {
        requestSpec.auth().preemptive().basic("root", "scriptkiddieguess");
        getUser(user.getName(), 401);
    }

    @Test
    public void t4_readUser() {
        requestSpec.auth().preemptive().basic(user.getName(), String.valueOf(user.getPassword()));
        Response response = getUser(user.getName(), 200);
        JsonPath jsonPath = response.jsonPath();
        revision = jsonPath.getString("_rev");
        assertTrue(revision.length() > 10);
        derivedKey = jsonPath.getString("derived_key");
        assertTrue(derivedKey.length() > 10);
        salt = jsonPath.getString("salt");
        assertTrue(salt.length() > 10);
    }

    @Test
    public void t5_updateUser() throws AddressException {
        requestSpec.auth().preemptive().basic(user.getName(), String.valueOf(user.getPassword()));
        String newEmailAddress = "fazil@fazil.de";
        user.setEmail(new InternetAddress(newEmailAddress));
        user.setRevision(revision);
        ResponseBody respBody = putUser(user, 200);
        JsonPath jsonPath = respBody.jsonPath();
        assertEquals(newEmailAddress, jsonPath.getString("email"));
        revision = jsonPath.getString("_rev");
    }

/*
    @Test
    public void t6_changePassword() {
        ResponseBody respBody = putUser(user, 200);
        JsonPath jsonPath = respBody.jsonPath();
        assertNotEquals(derivedKey, jsonPath.getString("derived_key"));
        assertNotEquals(salt, jsonPath.getString("salt"));
    }
*/

    @Test
    public void t7_removeUser() {
        requestSpec.auth().preemptive().basic(user.getName(), String.valueOf(user.getPassword()));
        deleteUser(user.getName(), revision, 204);
        getUser(user.getId(), 401);
    }

    @Test
    public void t8_checkUserRemoved() {
        requestSpec.auth().preemptive().basic(adminUserName, adminPassword);
        getUser(user.getId(), 404);
    }

}
