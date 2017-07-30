package de.bornemisza.users;

import static io.restassured.RestAssured.given;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import de.bornemisza.users.entity.User;

public class IntegrationTestBase {

    protected RequestSpecification requestSpec;
    protected URI baseUri = URI.create("https://www.bornemisza.de/users/");
    protected User user;


    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.println("\n>>>> Starting test: " + description.getMethodName() + "\n");
        }
    };

    @Before
    public void setUp() {
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
        user = new User();
        user.setName("Fazil Ongudar");
        user.setPassword("secret");
        List<String> roles = Arrays.asList(new String[]{"customer", "user"});
        user.setRoles(roles);
    }

    protected Response getUser(String docId, int expectedStatusCode) {
        requestSpec.accept(ContentType.JSON);
        return given().spec(requestSpec)
                .when().get(docId)
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected Response postUser(User user, int expectedStatusCode) {
        requestSpec.contentType(ContentType.JSON).body(user);
        return given().spec(requestSpec)
                .when().post("")
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

    protected ResponseBody putUser(User user, int expectedStatusCode) {
        requestSpec.contentType(ContentType.JSON).body(user);
        return given().spec(requestSpec)
                .when().put("")
                .then().statusCode(expectedStatusCode)
                .extract().response().getBody();
    }

    protected Response deleteUser(String userName, String rev, int expectedStatusCode) {
        requestSpec.accept(ContentType.ANY);
        return given().spec(requestSpec)
                .when().delete(userName + "/" + rev)
                .then().statusCode(expectedStatusCode)
                .extract().response();
    }

}
