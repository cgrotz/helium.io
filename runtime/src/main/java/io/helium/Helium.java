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

import io.helium.persistence.Persistence;
import io.helium.persistence.mapdb.MapDbService;
import io.helium.persistence.mapdb.PersistenceExecutor;
import io.helium.server.http.HttpServer;
import io.helium.server.mqtt.MqttServer;
import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.File;

/**
 * Main entry point for Helium
 *
 * @author Christoph Grotz
 */
public class Helium extends Verticle {
    @Override
    public void start(Future<Void> startedResult) {
        try {
            new File("helium").mkdirs();
            MapDbService.get();
            // Workers
            container.deployWorkerVerticle(Persistence.class.getName());
            container.deployWorkerVerticle(PersistenceExecutor.class.getName());

            // Channels
            container.deployVerticle(HttpServer.class.getName());
            //container.deployVerticle(MqttServer.class.getName());
            startedResult.complete();
        } catch (Exception e) {
            container.logger().error("Failed starting Helium", e);
            startedResult.setFailure(e);
        }
    }
}
