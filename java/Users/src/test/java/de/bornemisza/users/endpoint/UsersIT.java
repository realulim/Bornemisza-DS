package de.bornemisza.users.endpoint;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import de.bornemisza.users.IntegrationTestBase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UsersIT extends IntegrationTestBase {

    private static String revision;

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
    public void t2_readUser() {
        requestSpec.auth().none();
        Response response = getUser(user.getName(), 200);
        JsonPath jsonPath = response.jsonPath();
        revision = jsonPath.getString("_rev");
        assertTrue(revision.length() > 10);
        assertTrue(jsonPath.getString("derived_key").length() > 10);
        assertTrue(jsonPath.getString("salt").length() > 10);
    }

    @Test
    public void t3_updateUser() {
        user.setPassword(new char[]{'c','h','a','n','g','e','d'});
        user.setRevision(revision);
        ResponseBody respBody = putUser(user, 200);
        JsonPath jsonPath = respBody.jsonPath();
        revision = jsonPath.getString("_rev");
    }

    @Test
    public void t4_removeUser() {
        deleteUser(user.getName(), revision, 204);
        getUser(user.getId(), 404);
    }

}
