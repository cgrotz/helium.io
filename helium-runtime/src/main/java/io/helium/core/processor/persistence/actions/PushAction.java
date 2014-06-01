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

package io.helium.core.processor.persistence.actions;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.json.HashMapBackedNode;
import io.helium.persistence.Persistence;

import java.util.UUID;

public class PushAction {

    private Persistence persistence;

    public PushAction(Persistence persistence) {
        this.persistence = persistence;
    }

    public void handle(HeliumEvent event) {
        Path path = event.extractNodePath();
        Object payload;
        if (event.has(HeliumEvent.PAYLOAD)) {
            payload = event.get(HeliumEvent.PAYLOAD);
        } else {
            payload = new HashMapBackedNode();
        }

        String nodeName;
        if (event.has("name")) {
            nodeName = event.getString("name");
        } else {
            nodeName = UUID.randomUUID().toString().replaceAll("-", "");
        }
        if (path.isEmtpy()) {
            persistence.applyNewValue(event.getChangeLog(), event.getSequence(), event.getAuth(), new Path(nodeName),
                    -1, payload);
        } else {
            persistence.applyNewValue(event.getChangeLog(), event.getSequence(), event.getAuth(), path, -1, payload);
        }
    }

}
