package de.bornemisza.users.endpoint;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import de.bornemisza.users.IntegrationTestBase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UsersIT extends IntegrationTestBase {

    private static String revision, derivedKey, salt;

    @Test
    public void t0_userAccountCreationRequest() {
        deleteMails(user.getEmail());
        user.setPassword(userPassword.toCharArray());
        Response response = postUser(user, 202);
        assertEquals(0, response.getBody().prettyPrint().length());
    }

    @Test
    public void t1_confirmUser() {
        long start = System.currentTimeMillis();
        String confirmationLink = retrieveConfirmationLink(user.getEmail());
        assertTrue(confirmationLink.startsWith("https://"));
        System.out.println("Mail delivered after " + (System.currentTimeMillis() - start) + " ms.");
        Response response = clickConfirmationLink(confirmationLink, 200);
        JsonPath jsonPath = response.jsonPath();
        assertEquals(user.getEmail().toString(), jsonPath.getString("email"));
        assertEquals("******", jsonPath.getString("password"));

        // User is removed from Map by either Expiry or previous Confirmation, so this must fail
        response = clickConfirmationLink(confirmationLink, 404);
        assertEquals("User Account Creation Request does not exist - maybe expired?", response.print());
    }

    @Test
    public void t2_readUser_notAuthorized() {
        getUser(userName, 401);
    }

    @Test
    public void t3_readUser_unauthorized() {
        requestSpec.auth().preemptive().basic("root", "guessedpassword");
        getUser(userName, 401);
    }

    @Test
    public void t4_readUser() {
        requestSpec.auth().preemptive().basic(userName, userPassword);
        Response response = getUser(userName, 200);
        JsonPath jsonPath = response.jsonPath();
        revision = jsonPath.getString("_rev");
        assertTrue(revision.length() > 10);
        derivedKey = jsonPath.getString("derived_key");
        assertTrue(derivedKey.length() > 10);
        salt = jsonPath.getString("salt");
        assertTrue(salt.length() > 10);
    }

    @Test
    public void t5_changePassword() {
        requestSpec.auth().preemptive().basic(userName, userPassword);
        Response response = changePassword(userName, newUserPassword, 200);
        JsonPath jsonPath = response.getBody().jsonPath();
        String newRevision = jsonPath.getString("_rev");
        assertNotEquals(newRevision, revision);
        revision = newRevision;
    }

    @Test
    public void t6_removeUser() {
        requestSpec.auth().preemptive().basic(userName, newUserPassword);
        deleteUser(userName, 204);
        getUser(user.getId(), 401);
    }

    @Test
    public void t7_checkUserRemoved() {
        requestSpec.auth().preemptive().basic(adminUserName, adminPassword);
        getUser(user.getId(), 404);
    }

    @Test
    public void t8_removeNonExistingUser() {
        requestSpec.auth().preemptive().basic(adminUserName, adminPassword);
        deleteUser(userName, 404);
    }

//    @Test
//    public void t999_cleanup() {
//        requestSpec.auth().preemptive().basic(adminUserName, adminPassword);
//        Response response = getUser(userName, 200);
//        JsonPath jsonPath = response.jsonPath();
//        revision = jsonPath.getString("_rev");
//        deleteUser(userName, 204);
//    }

}
