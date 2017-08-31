package de.bornemisza.rest;

import static io.restassured.RestAssured.given;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import de.bornemisza.rest.entity.User;

public class IntegrationTestBase {

    protected static final String BASE_URI_PROP = "BASE.URI";
    protected static final String ADMIN_USERNAME_PROP = "ADMIN.USERNAME";
    protected static final String ADMIN_PASSWORD_PROP = "ADMIN.PASSWORD";
    protected RequestSpecification requestSpecUsers, requestSpecSessions;
    protected User user;

    protected String adminUserName, adminPassword, userName, userPassword, newUserPassword;
    protected InternetAddress newEmail;

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.println("\n>>>> Starting test: " + description.getMethodName() + "\n");
        }
    };

    @Before
    public void setUp() throws AddressException {
        String configuredUri = System.getProperty(BASE_URI_PROP);
        if (configuredUri == null) fail("Please configure " + BASE_URI_PROP + " in your build.properties");
        adminUserName = System.getProperty(ADMIN_USERNAME_PROP);
        if (adminUserName == null) fail("Please configure " + ADMIN_USERNAME_PROP + " in your build.properties");
        adminPassword = System.getProperty(ADMIN_PASSWORD_PROP);
        if (adminPassword == null) fail("Please configure " + ADMIN_PASSWORD_PROP + " in your build.properties");
        userName = "Fazil Ongudar";
        userPassword = "secret";
        newUserPassword = "changed";
        newEmail = new InternetAddress("fazil.changed@restmail.net");

        requestSpecUsers = new RequestSpecBuilder()
                .setBaseUri(URI.create(configuredUri + "users/"))
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
        requestSpecSessions = new RequestSpecBuilder()
                .setBaseUri(URI.create(configuredUri + "sessions/"))
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
        user = new User();
        user.setName(userName);
        user.setEmail(new InternetAddress("fazil.ongudar@restmail.net"));
        List<String> roles = Arrays.asList(new String[]{"customer", "user"});
        user.setRoles(roles);
    }

    protected Response getUser(String docId, int expectedStatusCode) {
        requestSpecUsers.accept(ContentType.JSON);
        return given(requestSpecUsers)
                .when().get(docId)
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected Response postUser(User user, int expectedStatusCode) {
        requestSpecUsers.contentType(ContentType.JSON).body(user);
        return given(requestSpecUsers)
                .when().post("")
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected Response putEmail(User user, int expectedStatusCode) {
        requestSpecUsers.contentType(ContentType.TEXT).body(user.getEmail().toString());
        return given(requestSpecUsers)
                .when().put("/" + user.getName() + "/email")
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected Response deleteUser(String userName, int expectedStatusCode) {
        requestSpecUsers.accept(ContentType.ANY);
        return given(requestSpecUsers)
                .when().delete(userName)
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected String retrieveConfirmationLink(InternetAddress recipient) {
        String mailUser = recipient.toString().split("@")[0];
        RequestSpecification localRequestSpec = new RequestSpecBuilder()
                .setBaseUri("https://restmail.net/mail/")
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();

        // wait up to 30 seconds for mail delivery
        String html = null;
        long mailDeliveryTimeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (html == null && System.currentTimeMillis() < mailDeliveryTimeout) {
            Response response = given(localRequestSpec)
                    .when().get(mailUser)
                    .then().statusCode(200)
                    .extract().response();
            html = response.jsonPath().getString("[0].html");
        }

        assertNotNull("No Mail for User " + mailUser, html);
        return html.split("href=\"")[1].split("\"")[0];
    }

    protected Response clickConfirmationLink(String confirmationLink, int expectedStatusCode) {
        return clickConfirmationLink(confirmationLink, expectedStatusCode, null);
    }

    protected Response clickConfirmationLink(String confirmationLink, int expectedStatusCode, BasicAuthCredentials creds) {
        String uuid = confirmationLink.substring(confirmationLink.lastIndexOf("/") + 1);
        RequestSpecification localRequestSpec = new RequestSpecBuilder()
                .setBaseUri(confirmationLink.substring(0, confirmationLink.length() - uuid.length()))
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
        if (creds != null) localRequestSpec.auth().preemptive().basic(creds.getUserName(), creds.getPassword());
        return given(localRequestSpec)
                .when().get(uuid)
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected Response changePassword(String userName, String password, int expectedStatusCode) {
        requestSpecUsers.accept(ContentType.JSON);
        return given(requestSpecUsers)
                .when().put(userName + "/password/" + password)
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected void deleteMails(InternetAddress recipient) {
        String mailUser = recipient.toString().split("@")[0];
        RequestSpecification localRequestSpec = new RequestSpecBuilder()
                .setBaseUri("https://restmail.net/mail/")
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
        given(localRequestSpec)
                .when().delete(mailUser)
                .then().statusCode(200);
    }

    protected Response getNewSession() {
        requestSpecSessions.accept(ContentType.JSON);
        return given(requestSpecSessions)
                .when().get("new")
                .then().extract().response();
    }

    protected Response getActiveSession(String cookie) {
        requestSpecSessions.accept(ContentType.JSON).cookie(cookie);
        return given(requestSpecSessions)
                .when().get("active")
                .then().extract().response();
    }

}
