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

package io.helium.persistence.actions;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

public class Post extends CommonPersistenceVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(Post.class);

    public void handle(Message<JsonObject> msg) {
        long start = System.currentTimeMillis();
        HeliumEvent event = HeliumEvent.of(msg.body());
        Path path = event.extractNodePath();
        Object payload;
        if (event.containsField(HeliumEvent.PAYLOAD)) {
            payload = event.getValue(HeliumEvent.PAYLOAD);
        } else {
            payload = new JsonObject();
        }

        String nodeName;
        if (event.containsField("name")) {
            nodeName = event.getString("name");
        } else {
            nodeName = UUID.randomUUID().toString().replaceAll("-", "");
        }
        if (path.isEmtpy()) {
            applyNewValue(event.getAuth(), new Path(nodeName), payload, changeLog -> {
                msg.reply(changeLog);
                LOGGER.info("Post Action took: " + (System.currentTimeMillis() - start) + "ms");
            });
        } else {
            applyNewValue(event.getAuth(), path, payload, changeLog -> {
                msg.reply(changeLog);
                LOGGER.info("Post Action took: " + (System.currentTimeMillis() - start) + "ms");
            });
        }
    }
}
