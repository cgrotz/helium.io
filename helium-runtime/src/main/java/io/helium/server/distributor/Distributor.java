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

package io.helium.server.distributor;

import com.google.common.collect.Sets;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.changelog.ChangeLog;
import io.helium.server.Endpoint;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class Distributor extends Verticle {

    public static final String DISTRIBUTE_HELIUM_EVENT = "distribute.helium.event";
    public static final String DISTRIBUTE_EVENT = "distribute.event";
    public static final String DISTRIBUTE_CHANGE_LOG = "distribute.change.log";

    @Override
    public void start() {
        vertx.eventBus().registerHandler(DISTRIBUTE_HELIUM_EVENT, this::distributeHeliumEvent);
        vertx.eventBus().registerHandler(DISTRIBUTE_EVENT, this::distributeEvent);
        vertx.eventBus().registerHandler(DISTRIBUTE_CHANGE_LOG, this::distributeChangeLog);
    }

    public void distributeHeliumEvent(Message<JsonObject> msg) {
        HeliumEvent event = HeliumEvent.of(msg.body());
        if (!event.isFromHistory()) {
            for (Endpoint handler : Sets.newHashSet(Endpoints.get().endpoints())) {
                handler.distribute(event);
            }
        }
    }

    public void distributeEvent(Message<JsonObject> message) {
        for (Endpoint handler : Sets.newHashSet(Endpoints.get().endpoints())) {
            handler.distributeEvent(new Path(HeliumEvent.extractPath(message.body().getString("path"))), message.body().getObject("payload"));
        }
    }


    public void distributeChangeLog(Message<JsonArray> event) {
        ChangeLog changeLog = ChangeLog.of(event.body());
        for (Endpoint endpoint : Endpoints.get().endpoints()) {
            endpoint.distributeChangeLog(changeLog);
        }
    }
}
