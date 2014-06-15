package io.helium;

import io.helium.authorization.Authorizator;
import io.helium.persistence.EventSource;
import io.helium.persistence.Persistor;
import io.helium.server.http.HttpServer;
import io.helium.server.mqtt.MqttServer;
import org.junit.Test;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

import static com.jayway.restassured.RestAssured.*;

/**
 * Created by Christoph Grotz on 14.06.14.
 */
public class SimpleTest extends TestVerticle {

    @Test
    public void requestHeliumJs() {
        container.deployWorkerVerticle(Authorizator.class.getName(), container.config());
        container.deployWorkerVerticle(EventSource.class.getName(), container.config());
        container.deployWorkerVerticle(Persistor.class.getName(), container.config());

        // Channels
        container.deployVerticle(HttpServer.class.getName(), container.config());
        container.deployVerticle(MqttServer.class.getName(), container.config());

        get("/helium.js").then().assertThat().statusCode(200);
        VertxAssert.testComplete();
    }
}
