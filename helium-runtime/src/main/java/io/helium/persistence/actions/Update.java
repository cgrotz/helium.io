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

import io.helium.authorization.Authorizator;
import io.helium.authorization.Operation;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;
import io.helium.persistence.Persistence;
import io.helium.persistence.mapdb.Node;
import io.helium.persistence.mapdb.NodeFactory;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.Optional;

public class Update extends CommonPersistenceVerticle {

    @Override
    public void start() {
        vertx.eventBus().registerHandler( Persistence.UPDATE, this::handle );
    }

    public void handle(Message<JsonObject> msg) {
        HeliumEvent event = HeliumEvent.of(msg.body());
        Path path = event.extractNodePath();
        if (event.containsField(HeliumEvent.PAYLOAD)) {
            Object obj = event.getValue(HeliumEvent.PAYLOAD);
            updateValue(event, event.getAuth(), path, obj);
        } else {
            delete(event, event.getAuth(), path);
        }
    }

    public void updateValue(HeliumEvent heliumEvent, Optional<JsonObject> auth, Path path, Object payload) {
        vertx.eventBus().send(Authorizator.CHECK,
                Authorizator.check(Operation.WRITE, auth, path, payload),
                new Handler<Message<Boolean>>() {
                    @Override
                    public void handle(Message<Boolean> event) {
                        if (event.body()) {
                            Node node;
                            boolean created = false;
                            if (!exists(path)) {
                                created = true;
                            }
                            Node parent;
                            if (exists(path.parent())) {
                                parent = Node.of(path.parent());
                            } else {
                                parent = Node.of(path.parent().parent());
                            }
                            ChangeLog log = heliumEvent.getChangeLog();
                            if (payload instanceof Node) {
                                if (parent.has(path.lastElement())) {
                                    node = parent.getNode(path.lastElement());
                                } else {
                                    node = Node.of(path.append(path.lastElement()));
                                    parent.put(path.lastElement(), node);
                                }
                                node.populate(new ChangeLogBuilder(log, path, path.parent(), node), (Node) payload);
                                if (created) {
                                    log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                            .parent(), payload, false, 0);
                                } else {
                                    log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                                            .parent(), payload, false, 0);
                                }
                            } else {
                                parent.putWithIndex(path.lastElement(), payload);

                                if (created) {
                                    log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                            .parent(), payload, false, 0);
                                } else {
                                    log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                                            .parent(), payload, false, 0);
                                    log.addValueChangedLogEntry(path.lastElement(), path, path.parent(), payload);
                                }
                                log.addChildChangedLogEntry(path.parent().lastElement(), path.parent().parent(),
                                        path.parent().parent().parent(), parent, false, 0);

                            }
                            NodeFactory.get().getDb().commit();
                        }
                    }
                }
        );
    }
}
