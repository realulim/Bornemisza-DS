package de.bornemisza.status;

import static io.restassured.RestAssured.*;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.Matchers.*;

public class HealthCheckIT {
    
    protected RequestSpecification requestSpec;

    public HealthCheckIT() {
    }

    @Before
    public void setUp() {
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(URI.create("https://www.bornemisza.de/status"))
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
    }

    @Test
    public void getHealthReport() {
        requestSpec.contentType(ContentType.ANY);
        Response response = given().spec(requestSpec)
                .when().get("")
                .then().statusCode(200).and().contentType(ContentType.TEXT)
                .and().body(containsString("Status: OK"))
                .extract().response();
        assertTrue(response.getBody().asString().contains("Status: OK"));
    }

}
