package de.bornemisza.users.endpoint;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;

import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.bornemisza.users.IntegrationTestBase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UsersIT extends IntegrationTestBase {

    private static String revision;

    @Test
    public void t0_addUser() {
        Response response = postUser(user, 202);
        System.out.println("Content-Type: " + response.contentType());
        System.out.println(response.prettyPrint());
    }

    @Test
    public void t1_confirmUser() {
        Response response = clickConfirmationLink(user.getEmail());
        JsonPath jsonPath = response.jsonPath();
        System.out.println(jsonPath.prettyPrint());
        System.out.println(response.prettyPrint());
    }

    @Test
    public void t2_readUser() {
        requestSpec.auth().none();
        Response response = getUser(user.getName(), 200);
        JsonPath jsonPath = response.jsonPath();
        revision = jsonPath.getString("_rev");
        assertTrue(revision.length() > 10);
    }

    @Test
    public void t3_udpateUser() {
        user.setPassword("changed");
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
