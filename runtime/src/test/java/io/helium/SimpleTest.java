package io.helium;

import com.google.common.io.Files;
import io.helium.authorization.Authorizator;
import io.helium.persistence.EventSource;
import io.helium.persistence.actions.*;
import io.helium.server.http.HttpServer;
import io.helium.server.mqtt.MqttServer;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

import java.io.File;

import static com.jayway.restassured.RestAssured.*;

/**
 * Created by Christoph Grotz on 14.06.14.
 */
public class SimpleTest extends TestVerticle {

    @Test
    public void requestHeliumJs() {
        File directory = Files.createTempDir();

        // Workers
        container.deployWorkerVerticle(Authorizator.class.getName(), container.config());
        container.deployWorkerVerticle(EventSource.class.getName(),
                container.config().getObject("journal", new JsonObject().putString("directory",(new File(directory,"journal").getAbsolutePath()))));

        container.deployVerticle(Get.class.getName(), container.config());
        container.deployVerticle(Post.class.getName(), container.config());
        container.deployVerticle(Put.class.getName(), container.config());
        container.deployVerticle(Delete.class.getName(), container.config());
        container.deployVerticle(Update.class.getName(), container.config());

        // Channels
        container.deployVerticle(HttpServer.class.getName(), container.config());
        container.deployVerticle(MqttServer.class.getName(),
                container.config().getObject("mqtt", new JsonObject().putString("directory",(new File(directory,"mqtt").getAbsolutePath()))));


        get("/helium.js").then().assertThat().statusCode(200);
        VertxAssert.testComplete();
    }


    @Test
    public void getRoot() {
        File directory = Files.createTempDir();

        // Workers
        container.deployWorkerVerticle(Authorizator.class.getName(), container.config());
        container.deployWorkerVerticle(EventSource.class.getName(),
                container.config().getObject("journal", new JsonObject().putString("directory",(new File(directory,"journal").getAbsolutePath()))));

        container.deployVerticle(Get.class.getName(), container.config());
        container.deployVerticle(Post.class.getName(), container.config());
        container.deployVerticle(Put.class.getName(), container.config());
        container.deployVerticle(Delete.class.getName(), container.config());
        container.deployVerticle(Update.class.getName(), container.config());

        // Channels
        container.deployVerticle(HttpServer.class.getName(), container.config());
        container.deployVerticle(MqttServer.class.getName(),
                container.config().getObject("mqtt", new JsonObject().putString("directory",(new File(directory,"mqtt").getAbsolutePath()))));


        get("/").then().assertThat().statusCode(200);
        VertxAssert.testComplete();
    }
}
