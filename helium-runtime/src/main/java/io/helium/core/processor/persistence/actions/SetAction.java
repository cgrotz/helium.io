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
import io.helium.json.Node;
import io.helium.persistence.Persistence;

public class SetAction {

    private Persistence persistence;

    public SetAction(Persistence persistence) {
        this.persistence = persistence;
    }

    public void handle(HeliumEvent event) {
        Path path = event.extractNodePath();
        Node payload;
        if (event.has(HeliumEvent.PAYLOAD)) {
            Object obj = event.get(HeliumEvent.PAYLOAD);
            if (obj == Node.NULL || obj == null) {
                persistence.remove(event.getChangeLog(), event.getAuth(), path);
            } else if (obj instanceof Node) {
                payload = (Node) obj;
                if (payload instanceof Node) {
                    if (event.hasPriority()) {
                        persistence.applyNewValue(event.getChangeLog(), event.getAuth(), path,
                                event.getPriority(), obj);
                    } else {
                        persistence.applyNewValue(event.getChangeLog(), event.getAuth(), path, -1,
                                obj);
                    }
                }
            } else if (obj == null || obj == Node.NULL) {
                persistence.remove(event.getChangeLog(), event.getAuth(), path);
            } else {
                if (event.hasPriority()) {
                    persistence.applyNewValue(event.getChangeLog(), event.getAuth(), path,
                            event.getPriority(), obj);
                } else {
                    persistence.applyNewValue(event.getChangeLog(), event.getAuth(), path, -1, obj);
                }
            }
        } else {
            persistence.remove(event.getChangeLog(), event.getAuth(), path);
        }

    }

}
