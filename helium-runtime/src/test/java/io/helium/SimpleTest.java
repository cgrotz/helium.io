package io.helium;

import com.google.common.io.Files;
import io.helium.authorization.Authorizator;
import io.helium.persistence.EventSource;
import io.helium.persistence.Persistor;
import io.helium.persistence.mapdb.NodeFactory;
import io.helium.server.http.HttpServer;
import io.helium.server.mqtt.MqttServer;
import org.junit.Test;
import org.mapdb.DB;
import org.vertx.java.core.Handler;
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
        container.deployWorkerVerticle(Persistor.class.getName(),
                container.config().getObject("mapdb", new JsonObject().putString("directory",(new File(directory,"nodes").getAbsolutePath()))),
                1, true,
                event -> {
                    vertx.setPeriodic(1000, new Handler<Long>() {
                        @Override
                        public void handle(Long event) {
                            DB db = NodeFactory.get().getDb();
                            if( db != null)
                                db.commit();
                        }
                    });
                });

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
        container.deployWorkerVerticle(Persistor.class.getName(),
                container.config().getObject("mapdb", new JsonObject().putString("directory",(new File(directory,"nodes").getAbsolutePath()))),
                1, true,
                event -> {
                    vertx.setPeriodic(1000, new Handler<Long>() {
                        @Override
                        public void handle(Long event) {
                            DB db = NodeFactory.get().getDb();
                            if( db != null)
                                db.commit();
                        }
                    });
                });

        // Channels
        container.deployVerticle(HttpServer.class.getName(), container.config());
        container.deployVerticle(MqttServer.class.getName(),
                container.config().getObject("mqtt", new JsonObject().putString("directory",(new File(directory,"mqtt").getAbsolutePath()))));


        get("/").then().assertThat().statusCode(200);
        VertxAssert.testComplete();
    }
}
