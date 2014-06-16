/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium;

import io.helium.authorization.Authorizator;
import io.helium.persistence.EventSource;
import io.helium.persistence.Persistence;
import io.helium.persistence.mapdb.NodeFactory;
import io.helium.server.http.HttpServer;
import io.helium.server.mqtt.MqttServer;
import org.mapdb.DB;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Main entry point for Helium
 *
 * @author Christoph Grotz
 */
public class Helium extends Verticle {
    @Override
    public void start(Future<Void> startedResult) {
        try {
            // Workers
            container.deployWorkerVerticle(Authorizator.class.getName(), defaultAuthorizatorConfig());
            container.deployWorkerVerticle(EventSource.class.getName(),
                    container.config().getObject("journal", defaultJournalConfig()));
            container.deployWorkerVerticle(Persistence.class.getName(),
                container.config().getObject("mapdb", createPersistenceDefaultConfig()),
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
            container.deployVerticle(HttpServer.class.getName(), container.config().getObject("http", defaultHttpConfig() ));
            container.deployVerticle(MqttServer.class.getName(),
                    container.config().getObject("mqtt", defaultMqttConfig()));

            startedResult.complete();
        } catch (Exception e) {
            container.logger().error("Failed starting Helium", e);
            startedResult.setFailure(e);
        }
    }

    private JsonObject defaultMqttConfig() {
        return new JsonObject().putNumber("port", 1883).putString("directory", "helium/mqtt");
    }

    private JsonObject defaultHttpConfig() {
        return new JsonObject().putNumber("port", 8080)
                .putString("basepath", "http://localhost:8080/");
    }

    private JsonObject defaultAuthorizatorConfig() {
        return container.config();
    }

    private JsonObject createPersistenceDefaultConfig() {
        return new JsonObject().putString("directory", "helium/nodes");
    }

    private JsonObject defaultJournalConfig() {
        return new JsonObject().putString("directory", "helium/journal");
    }
}
