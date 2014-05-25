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

package io.helium.core.processor.authorization;

import com.lmax.disruptor.EventHandler;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;

public class AuthorizationProcessor implements EventHandler<HeliumEvent> {

    private Authorization authorization;

    private Persistence persistence;

    public AuthorizationProcessor(Authorization authorization,
                                  Persistence persistence) {
        this.authorization = authorization;
        this.persistence = persistence;
    }

    @Override
    public void onEvent(HeliumEvent event, long sequence, boolean endOfBatch) {
        Path path = event.extractNodePath();
        if (event.getType() == HeliumEventType.PUSH) {
            InMemoryDataSnapshot root = new InMemoryDataSnapshot(
                    persistence.get(null));
            InMemoryDataSnapshot data = new InMemoryDataSnapshot(
                    event.has(HeliumEvent.PAYLOAD) ? event.get(HeliumEvent.PAYLOAD)
                            : null
            );
            authorization.authorize(Operation.WRITE, getAuth(event), root, path, data);
        } else if (event.getType() == HeliumEventType.SET) {
            if (event.has(HeliumEvent.PAYLOAD)
                    && event.get(HeliumEvent.PAYLOAD) == Node.NULL) {
                InMemoryDataSnapshot root = new InMemoryDataSnapshot(
                        persistence.get(null));
                InMemoryDataSnapshot data = new InMemoryDataSnapshot(
                        event.get(HeliumEvent.PAYLOAD));
                authorization.authorize(Operation.WRITE, getAuth(event), root, path, data);
            } else {
                InMemoryDataSnapshot root = new InMemoryDataSnapshot(
                        persistence.get(null));
                authorization.authorize(Operation.WRITE, getAuth(event), root, path, null);
            }
        }
    }

    private Node getAuth(HeliumEvent event) {
        if (event.has(HeliumEvent.AUTH)) {
            return event.getNode(HeliumEvent.AUTH);
        } else {
            return new Node();
        }
    }
}
