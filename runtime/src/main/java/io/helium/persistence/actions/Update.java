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
import io.helium.persistence.mapdb.Node;
import io.helium.persistence.mapdb.MapDbService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Optional;

public class Update extends CommonPersistenceVerticle {

    public static final String UPDATE = "io.helium.persistor.update";

    @Override
    public void start() {
        vertx.eventBus().registerHandler( UPDATE, this::handle );
    }

    public void handle(Message<JsonObject> msg) {
        long start = System.currentTimeMillis();
        HeliumEvent event = HeliumEvent.of(msg.body());
        Path path = event.extractNodePath();
        if (event.containsField(HeliumEvent.PAYLOAD)) {
            Object obj = event.getValue(HeliumEvent.PAYLOAD);
            updateValue(event.getAuth(), path, obj, changeLog -> {
                msg.reply( changeLog );
                container.logger().info("Update Action took: "+(System.currentTimeMillis()-start)+"ms");
            });
        } else {
            delete(event.getAuth(), path, changeLog -> {
                msg.reply(changeLog);
                container.logger().info("Update Action took: "+(System.currentTimeMillis()-start)+"ms");
            });
        }
    }

    public void updateValue(Optional<JsonObject> auth, Path path, Object payload, Handler<ChangeLog> handler) {
        Authorizator.get().check(Operation.WRITE, auth, path, payload, (Boolean event) -> {
                if (event) {
                    Node node;
                    boolean created = false;
                    if (!exists(path)) {
                        created = true;
                    }
                    Node parent;
                    if (exists(path.parent())) {
                        parent = MapDbService.get().of(path.parent());
                    } else {
                        parent = MapDbService.get().of(path.parent().parent());
                    }
                    ChangeLog log = new ChangeLog(new JsonArray());
                    if (payload instanceof Node) {
                        if (parent.has(path.lastElement())) {
                            node = parent.getNode(path.lastElement());
                        } else {
                            node = MapDbService.get().of(path.append(path.lastElement()));
                        }
                        populateNode(node, new ChangeLogBuilder(log, path, path.parent(), node), (Node) payload);
                        if (created) {
                            log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                    .parent(), payload, false, 0);
                        } else {
                            log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                                    .parent(), payload, false, 0);
                        }
                    } else {
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
                    handler.handle(log);
                }
            }
        );
    }

    private void populateNode(Node node, ChangeLogBuilder logBuilder, Node payload) {
        for (String key : payload.keys()) {
            Object value = payload.get(key);
            if (value instanceof Node) {
                Node childNode = MapDbService.get().of(node.getPathToNode().append(key));
                populateNode(childNode, logBuilder.getChildLogBuilder(key), (Node) value);
                if (node.has(key)) {
                    logBuilder.addNew(key, childNode);
                } else {
                    logBuilder.addChangedNode(key, childNode);
                }
            } else {
                if (node.has(key)) {
                    logBuilder.addChange(key, value);
                } else {
                    logBuilder.addNew(key, value);
                }
                if (value == null) {
                    logBuilder.addDeleted(key, node.get(key));
                }
            }
        }
    }
}
