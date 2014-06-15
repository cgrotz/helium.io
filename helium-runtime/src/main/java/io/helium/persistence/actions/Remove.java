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
import io.helium.persistence.infinispan.InfinispanPersistence;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class Remove {

    private InfinispanPersistence persistence;

    public Remove(InfinispanPersistence persistence) {
        this.persistence = persistence;
    }

    public void handle(Message<JsonObject> msg) {
        HeliumEvent event = HeliumEvent.of(msg.body());
        Path path = event.extractNodePath();

        persistence.remove(event, event.getAuth(), path);

    }
}
