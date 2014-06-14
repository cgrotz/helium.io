package io.helium;

import org.junit.Test;
import org.vertx.testtools.TestVerticle;

import static com.jayway.restassured.RestAssured.*;

/**
 * Created by Christoph Grotz on 14.06.14.
 */
public class SimpleTest extends TestVerticle {

    @Test
    public void test1() {
        container.deployVerticle(Helium.class.getName());

        post("/test").then().assertThat().statusCode(200);
    }
}
