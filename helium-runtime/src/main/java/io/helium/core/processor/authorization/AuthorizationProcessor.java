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
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Optional;

public class AuthorizationProcessor implements EventHandler<HeliumEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationProcessor.class);

    private Authorization authorization;

    private Persistence persistence;

    public AuthorizationProcessor(Authorization authorization,
                                  Persistence persistence) {
        this.authorization = authorization;
        this.persistence = persistence;
    }

    @Override
    public void onEvent(HeliumEvent event, long sequence, boolean endOfBatch) {
        long startTime = System.currentTimeMillis();
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
                    && event.get(HeliumEvent.PAYLOAD) == HashMapBackedNode.NULL) {
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
        LOGGER.info("onEvent("+sequence+") "+(System.currentTimeMillis()-startTime)+"ms; event processing time "+(System.currentTimeMillis()-event.getLong("creationDate"))+"ms");
    }

    private Optional<Node> getAuth(HeliumEvent event) {
        if (event.has(HeliumEvent.AUTH)) {
            return Optional.of(event.getNode(HeliumEvent.AUTH));
        } else {
            return Optional.empty();
        }
    }

    public static Node decode(String authorizationToken) {
        String decodedAuthorizationToken = new String(Base64.getDecoder().decode(authorizationToken.substring(6)));
        String username = decodedAuthorizationToken.substring(0, decodedAuthorizationToken.indexOf(":"));
        String password = decodedAuthorizationToken.substring(decodedAuthorizationToken.indexOf(":")+1);
        return new HashMapBackedNode().put("username",username).put("password", password);
    }
}
