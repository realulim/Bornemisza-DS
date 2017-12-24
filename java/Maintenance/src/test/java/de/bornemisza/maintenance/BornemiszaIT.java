package de.bornemisza.maintenance;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.User;
import de.bornemisza.rest.security.BasicAuthCredentials;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BornemiszaIT extends IntegrationTestBase {

    private static String revision, cookie, ctoken;

    @Test
    public void t00_userAccountCreationRequest() {
        deleteMails(user.getEmail());
        user.setPassword(userPassword.toCharArray());
        Response response = postUser(user, 202);
        assertEquals(0, response.getBody().prettyPrint().length());
    }

    @Test
    public void t01_confirmUser() {
        long start = System.currentTimeMillis();
        String confirmationLink = retrieveConfirmationLink(user.getEmail());
        assertTrue(confirmationLink.startsWith("https://"));
        System.out.println("Mail delivered after " + (System.currentTimeMillis() - start) + " ms.");
        String apiLink = convertToApiLink(confirmationLink, "user");
        Response response = clickApiLink(apiLink, 200);
        JsonPath jsonPath = response.jsonPath();
        assertEquals(user.getEmail().toString(), jsonPath.getString("email"));
        assertNull(jsonPath.getString("password"));

        /* Request is removed from Map by either Expiry or previous Confirmation, so this must fail */
        response = clickApiLink(apiLink, 404);
        assertEquals("User Account Creation Request does not exist - maybe expired?", response.print());
    }

    @Test
    public void t02_userAccountCreationRequest_userAlreadyExists() {
        user.setPassword(userPassword.toCharArray());
        Response response = postUser(user, 409);
        assertEquals(user.getName() + " already exists!", response.getBody().print());
    }

    @Test
    public void t03_userAccountCreationRequest_emailAlreadyExists() {
        User userWithExistingEmail = new User();
        userWithExistingEmail.setEmail(user.getEmail());
        userWithExistingEmail.setName("Not " + user.getName());
        userWithExistingEmail.setPassword(new char[] {'p', 'w'});
        userWithExistingEmail.setRoles(user.getRoles());
        Response response = postUser(userWithExistingEmail, 409);
        assertEquals(user.getEmail().getAddress() + " already exists!", response.getBody().print());
    }

    @Test
    public void t04_readUser_notAuthorized() {
        getUser(userName, 401);
    }

    @Test
    public void t05_readUser_unauthorized() {
        requestSpecUsers.auth().preemptive().basic("root", "guessedpassword");
        getUser(userName, 401);
    }

    @Test
    public void t06_createSession() {
        requestSpecSessions.auth().preemptive().basic(userName, userPassword);
        Response response = getNewSession();
        cookie = response.header("Set-Cookie");
        ctoken = response.header("C-Token");
        assertNotNull(cookie, ctoken);
        assertTrue(cookie.startsWith("AuthSession="));
        assertFalse(cookie.startsWith("AuthSession=;"));
        assertNotEquals(cookie, ctoken);
    }

    @Test
    public void t07_readUser() {
        requestSpecUsers.header(HttpHeaders.COOKIE, cookie).header(HttpHeaders.CTOKEN, ctoken);
        Response response = getUser(userName, 200);
        JsonPath jsonPath = response.jsonPath();
        revision = jsonPath.getString("_rev");
        assertTrue(revision.length() > 10);
        assertNull(jsonPath.getString("password"));
    }

    @Test
    public void t08_getUuids() {
        int count = 3;
        Response response = getUuids(cookie, ctoken, count, 200);
        JsonPath jsonPath = response.jsonPath();
        List<String> uuids = jsonPath.getList("uuids");
        assertEquals(count, uuids.size());
        List<String> definedColors = Arrays.asList(new String[] { "LightSeaGreen", "Crimson", "Gold", "RoyalBlue", "LightSalmon"});
        assertTrue(definedColors.contains(response.getHeader("AppServer")));
        assertTrue(definedColors.contains(response.getHeader("DbServer")));
    }

    @Test
    public void t09_loadColors() {
        Response response = loadColors(cookie, ctoken, 200);
        JsonPath jsonPath = response.jsonPath();
        List<Map<String, String>> rows = jsonPath.getList("rows");
        assertEquals(1, rows.size());
        Map<String, String> row = rows.get(0);
        assertEquals("LightSeaGreen", row.get("key"));
        assertEquals("3", row.get("value"));
    }

    @Test
    public void t10_changeEmailRequest() {
        deleteMails(newEmail);
        user.setEmail(newEmail);
        Response response = putEmail(cookie, ctoken, user, 202);
        assertEquals(0, response.getBody().prettyPrint().length());
    }

    @Test
    public void t11_confirmEmail() {
        BasicAuthCredentials creds = new BasicAuthCredentials(userName, userPassword);
        long start = System.currentTimeMillis();
        String confirmationLink = retrieveConfirmationLink(newEmail);
        assertTrue(confirmationLink.startsWith("https://"));
        System.out.println("Mail delivered after " + (System.currentTimeMillis() - start) + " ms.");
        String apiLink = convertToApiLink(confirmationLink, "email");
        Response response = clickApiLink(apiLink, 200, creds);
        JsonPath jsonPath = response.jsonPath();
        assertEquals(newEmail.toString(), jsonPath.getString("email"));

        /* Request is removed from Map by either Expiry or previous Confirmation, so this must fail */
        response = clickApiLink(apiLink, 404, creds);
        assertEquals("E-Mail Change Request does not exist - maybe expired?", response.print());
    }

    @Test
    public void t12_endSession() {
        Response response = endSession(200);
        assertEquals("AuthSession=; Version=1; Path=/; HttpOnly; Secure", response.getHeader("Set-Cookie"));
        assertNull(response.getHeader("C-Token"));
        assertEquals(0, response.getBody().prettyPrint().length());
    }

    @Test
    public void t13_getUuidsWithoutCookie() {
        Response response = getUuidsWithoutCookie(ctoken, 1, 401);
        assertEquals("Cookie or C-Token missing!", response.getBody().prettyPrint());
    }

    @Test
    public void t14_getUuidsWithoutCToken() {
        Response response = getUuidsWithoutCToken(cookie, 1, 401);
        assertEquals("Cookie or C-Token missing!", response.getBody().prettyPrint());
    }

    @Test
    public void t15_changePassword() {
        requestSpecUsers.auth().preemptive().basic(userName, userPassword);
        Response response = changePassword(userName, newUserPassword, 200);
        JsonPath jsonPath = response.getBody().jsonPath();
        String newRevision = jsonPath.getString("_rev");
        assertNotEquals(newRevision, revision);
        revision = newRevision;
    }

    @Test
    public void t16_removeUser() {
        requestSpecUsers.auth().preemptive().basic(userName, newUserPassword);
        deleteUser(userName, 204);
        getUser(userName, 401);
    }

    @Test
    public void t17_checkUserRemoved() {
        requestSpecUsers.auth().preemptive().basic(adminUserName, adminPassword);
        deleteUser(userName, 404);
    }

}
