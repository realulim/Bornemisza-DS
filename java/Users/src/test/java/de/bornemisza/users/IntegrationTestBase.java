package de.bornemisza.users;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static io.restassured.RestAssured.given;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import static org.junit.Assert.*;

import de.bornemisza.users.entity.User;

public class IntegrationTestBase {

    protected static final String BASE_URI_PROP = "BASE.URI";
    protected static final String USERNAME_PROP = "TESTUSER.USERNAME";
    protected static final String PASSWORD_PROP = "TESTUSER.PASSWORD";
    protected RequestSpecification requestSpec;
    protected URI baseUri;
    protected User user;

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
        String userName = System.getProperty(USERNAME_PROP);
        if (userName == null) fail("Please configure " + USERNAME_PROP + " in your build.properties");
        String password = System.getProperty(PASSWORD_PROP);
        if (password == null) fail("Please configure " + PASSWORD_PROP + " in your build.properties");
        baseUri = URI.create(configuredUri);
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
        requestSpec.auth().preemptive().basic(userName, password);
        user = new User();
        user.setName("Fazil Ongudar");
        user.setPassword("secret");
        user.setEmail(new InternetAddress("fazil.ongudar@restmail.net"));
        List<String> roles = Arrays.asList(new String[]{"customer", "user"});
        user.setRoles(roles);
    }

    protected Response getUser(String docId, int expectedStatusCode) {
        requestSpec.accept(ContentType.JSON);
        return given(requestSpec)
                .when().get(docId)
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected Response postUser(User user, int expectedStatusCode) {
        requestSpec.contentType(ContentType.JSON).body(user);
        return given(requestSpec)
                .when().post("")
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected ResponseBody putUser(User user, int expectedStatusCode) {
        requestSpec.contentType(ContentType.JSON).body(user);
        return given(requestSpec)
                .when().put("")
                .then().statusCode(expectedStatusCode)
                .extract().response().getBody();
    }

    protected Response deleteUser(String userName, String rev, int expectedStatusCode) {
        requestSpec.accept(ContentType.ANY);
        return given(requestSpec)
                .when().delete(userName + "/" + rev)
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected String retrieveConfirmationLink(InternetAddress recipient) {
        String mailUser = recipient.toString().split("@")[0];
        RequestSpecification localRequestSpec = new RequestSpecBuilder()
                .setBaseUri("https://restmail.net/mail/")
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

    protected Response clickConfirmationLink(String confirmationLink) {
        String uuid = confirmationLink.substring(confirmationLink.lastIndexOf("/") + 1);
        RequestSpecification localRequestSpec = new RequestSpecBuilder()
                .setBaseUri(confirmationLink.substring(0, confirmationLink.length() - uuid.length()))
                .build();
        return given(localRequestSpec)
                .when().get(uuid)
                .then().statusCode(200)
                .extract().response();
    }

    protected void deleteMails(InternetAddress recipient) {
        String mailUser = recipient.toString().split("@")[0];
        RequestSpecification localRequestSpec = new RequestSpecBuilder()
                .setBaseUri("https://restmail.net/mail/")
                .build();
        given(localRequestSpec)
                .when().delete(mailUser)
                .then().statusCode(200);
    }

}
