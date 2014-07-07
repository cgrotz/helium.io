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

package io.helium.server.http;

import io.helium.server.websocket.WebsocketEndpoint;
import org.vertx.java.core.Future;
import org.vertx.java.platform.Verticle;

public class HttpServer extends Verticle {

    @Override
    public void start(Future<Void> startedResult) {
        try {
            int port = container.config().getInteger("port", 8080);
            String basePath = container.config().getString("basepath", "http://localhost:8080/");

            vertx.createHttpServer()
                    .requestHandler(new RestHandler(vertx))
                    .websocketHandler(socket -> new WebsocketEndpoint(basePath, socket, vertx, container))
                    .listen(port, event -> startedResult.complete());
        }
        catch(Exception e) {
            startedResult.setFailure(e);
        }
    }
}