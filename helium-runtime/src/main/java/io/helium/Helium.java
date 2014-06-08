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
import io.helium.persistence.Persistor;
import io.helium.server.distributor.Distributor;
import io.helium.server.protocols.http.HttpServer;
import org.vertx.java.platform.Verticle;

/**
 * Main entry point for Helium
 *
 * @author Christoph Grotz
 */
public class Helium extends Verticle {

    @Override
    public void start() {
        try {
            // Workers
            container.deployWorkerVerticle(Authorizator.class.getName(), container.config());
            container.deployWorkerVerticle(Distributor.class.getName(), container.config());
            container.deployWorkerVerticle(EventSource.class.getName(), container.config());
            container.deployWorkerVerticle(Persistor.class.getName(), container.config());

            // Servers
            container.deployVerticle(HttpServer.class.getName(), container.config());

            /*
            vertx.setPeriodic(1000, event -> {
                MapDbBackedNode.getDb().commit();
                MapDbBackedNode.getDb().compact();
            });

            vertx.setPeriodic(5000, event -> {
                MapDbBackedNode.getDb().compact();
            });*/
        } catch (Exception e) {
            container.logger().error("Failed starting Helium", e);
        }
    }
}
