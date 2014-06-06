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

package io.helium.server.protocols.websocket;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import io.helium.authorization.Authorizator;
import io.helium.authorization.Operation;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.changelog.*;
import io.helium.persistence.DataSnapshot;
import io.helium.persistence.Persistor;
import io.helium.persistence.mapdb.MapDbBackedNode;
import io.helium.persistence.queries.QueryEvaluator;
import io.helium.server.Endpoint;
import io.helium.server.distributor.Distributor;
import io.helium.server.protocols.websocket.rpc.Rpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

public class WebsocketEndpoint implements Endpoint {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebsocketEndpoint.class);
    private final Vertx vertx;
    private Multimap<String, String> attached_listeners = HashMultimap.create();
    private String basePath;
    private Optional<JsonObject> auth = Optional.empty();
    private QueryEvaluator queryEvaluator;
    private ServerWebSocket socket;
    private List<HeliumEvent> disconnectEvents = Lists.newArrayList();

    private boolean open = true;

    private Rpc rpc;

    public WebsocketEndpoint(String basePath,
                             ServerWebSocket channel,
                             Vertx vertx) {
        this.socket = socket;
        this.vertx = vertx;
        this.basePath = basePath;
        this.queryEvaluator = new QueryEvaluator();

        this.rpc = new Rpc();
        this.rpc.register(this);
    }


    @Rpc.Method
    public void attachListener(@Rpc.Param("path") String path,
                               @Rpc.Param("event_type") String eventType) {
        LOGGER.info("attachListener");
        addListener(new Path(HeliumEvent.extractPath(path)), eventType);
        if ("child_added".equals(eventType)) {
            ChangeLog log = new ChangeLog();
            syncPath(log, new Path(HeliumEvent.extractPath(path)), this);
            distributeChangeLog(log);
        } else if ("value".equals(eventType)) {
            ChangeLog log = new ChangeLog();
            syncPropertyValue(log, new Path(HeliumEvent.extractPath(path)), this);
            distributeChangeLog(log);
        }
    }

    @Rpc.Method
    public void detachListener(@Rpc.Param("path") String path,
                               @Rpc.Param("event_type") String eventType) {
        LOGGER.info("detachListener");
        removeListener(new Path(HeliumEvent.extractPath(path)), eventType);
    }

    @Rpc.Method
    public void attachQuery(@Rpc.Param("path") String path, @Rpc.Param("query") String query) {
        LOGGER.info("attachQuery");
        addQuery(new Path(HeliumEvent.extractPath(path)), query);
        syncPathWithQuery(new ChangeLog(), new Path(HeliumEvent.extractPath(path)), this,
                new QueryEvaluator(), query);
    }

    @Rpc.Method
    public void detachQuery(@Rpc.Param("path") String path, @Rpc.Param("query") String query) {
        LOGGER.info("detachQuery");
        removeQuery(new Path(HeliumEvent.extractPath(path)), query);
    }

    @Rpc.Method
    public void event(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        LOGGER.info("event");
        vertx.eventBus().send(Distributor.DISTRIBUTE_EVENT, new JsonObject().putString("path", path).putObject("payload", data));
    }

    @Rpc.Method
    public void push(@Rpc.Param("path") String path, @Rpc.Param("name") String name,
                     @Rpc.Param("data") JsonObject data) {
        LOGGER.info("push");
        HeliumEvent event = new HeliumEvent(HeliumEventType.PUSH, path + "/" + name, data);
        if(auth.isPresent())
            event.setAuth(auth.get());
        vertx.eventBus().send(Distributor.DISTRIBUTE_HELIUM_EVENT, event);
    }

    @Rpc.Method
    public void set(@Rpc.Param("path") String path, @Rpc.Param("data") Object data) {
        LOGGER.info("set");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SET, path, data);
        if(auth.isPresent())
            event.setAuth(auth.get());
        vertx.eventBus().send(Distributor.DISTRIBUTE_HELIUM_EVENT, event);
    }

    @Rpc.Method
    public void update(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        LOGGER.info("update");
        HeliumEvent event = new HeliumEvent(HeliumEventType.UPDATE, path, data);
        if(auth.isPresent())
            event.setAuth(auth.get());
        vertx.eventBus().send(Distributor.DISTRIBUTE_HELIUM_EVENT, event);
    }

    @Rpc.Method
    public void pushOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("name") String name,
                                 @Rpc.Param("payload") JsonObject payload) {
        LOGGER.info("pushOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.PUSH, path + "/" + name,
                payload);
        if(auth.isPresent())
            event.setAuth(auth.get());
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void setOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        LOGGER.info("setOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SET, path, data);
        if(auth.isPresent())
            event.setAuth(auth.get());
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void updateOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        LOGGER.info("updateOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.UPDATE, path, data);
        if(auth.isPresent())
            event.setAuth(auth.get());
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void removeOnDisconnect(@Rpc.Param("path") String path) {
        LOGGER.info("removeOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.REMOVE, path);
        if(auth.isPresent())
            event.setAuth(auth.get());
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void authenticate(@Rpc.Param("username") String username, @Rpc.Param("password") String password) {
        LOGGER.info("authenticate");
        extractAuthentication(username, password, new Handler<Optional<JsonObject>>() {
            @Override
            public void handle(Optional<JsonObject> event) {
                auth = event;
            }
        });
    }

    private void extractAuthentication(String username, String password, Handler<Optional<JsonObject>> handler) {
        vertx.eventBus().send(Persistor.GET, Persistor.get(Path.of("/users")), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject users = event.body();
                for (String key : users.getFieldNames()) {
                    Object value = users.getObject(key);
                    if (value instanceof JsonObject) {
                        JsonObject node = (JsonObject) value;
                        if (node.containsField("username") && node.containsField("password")) {
                            String localUsername = node.getString("username");
                            String localPassword = node.getString("password");
                            if (username.equals(localUsername) && password.equals(localPassword)) {
                                handler.handle(Optional.of(node));
                                return;
                            }
                        }
                    }
                }
                handler.handle(Optional.empty());
            }
        });
    }

    public void handle(String msg) {
        rpc.handle(msg, this);
    }

    public void distribute(HeliumEvent event) {
        if (open) {
            long startTime = System.currentTimeMillis();
            if (event.getType() == HeliumEventType.EVENT) {
                JsonObject jsonObject;
                Object object = event.getValue(HeliumEvent.PAYLOAD);
                if (object instanceof JsonObject) {
                    jsonObject = (JsonObject) object;
                    distributeEvent(event.extractNodePath(), jsonObject);
                } else if (object instanceof String) {
                    jsonObject = new JsonObject(HeliumEvent.PAYLOAD);
                    distributeEvent(event.extractNodePath(), new JsonObject((String) object));
                }
            } else {
                processQuery(event);
                ChangeLog changeLog = event.getChangeLog();
                distributeChangeLog(changeLog);
            }
            LOGGER.info("distribute "+(System.currentTimeMillis()-startTime)+"ms; event processing time "+(System.currentTimeMillis()-event.getLong("creationDate"))+"ms");
        }
    }

    public void distributeChangeLog(ChangeLog changeLog) {
        long startTime = System.currentTimeMillis();
        changeLog.forEach(logE -> {
            if (logE instanceof ChildAddedLogEvent) {
                ChildAddedLogEvent logEvent = (ChildAddedLogEvent) logE;
                if (hasListener(logEvent.getPath(), CHILD_ADDED)) {
                    fireChildAdded(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren()
                    );
                }
            }
            if (logE instanceof ChildChangedLogEvent) {
                ChildChangedLogEvent logEvent = (ChildChangedLogEvent) logE;
                if (hasListener(logEvent.getPath(), CHILD_CHANGED)) {
                    fireChildChanged(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren()
                    );
                }
            }
            if (logE instanceof ValueChangedLogEvent) {
                ValueChangedLogEvent logEvent = (ValueChangedLogEvent) logE;
                if (hasListener(logEvent.getPath(), VALUE)) {
                    fireValue(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue());
                }
            }
            if (logE instanceof ChildRemovedLogEvent) {
                ChildRemovedLogEvent logEvent = (ChildRemovedLogEvent) logE;
                if (hasListener(logEvent.getPath(), CHILD_REMOVED)) {
                    fireChildRemoved(logEvent.getPath(), logEvent.getName(), logEvent.getValue());
                }
            }
        });
        LOGGER.info("distributeChangeLog "+(System.currentTimeMillis()-startTime)+"ms");
    }

    private void processQuery(HeliumEvent event) {
        Path nodePath = event.extractNodePath();
        if (MapDbBackedNode.exists(nodePath)) {
            nodePath = nodePath.parent();
        }

        if (hasQuery(nodePath.parent())) {
            for (Entry<String, String> queryEntry : queryEvaluator.getQueries()) {
                if (event.getPayload() != null) {
                    JsonObject value = MapDbBackedNode.of(nodePath).toJsonObject();
                    JsonObject parent = MapDbBackedNode.of(nodePath.parent()).toJsonObject();
                    boolean matches = queryEvaluator.evaluateQueryOnValue(value, queryEntry.getValue());
                    boolean containsNode = queryEvaluator.queryContainsNode(new Path(queryEntry.getKey()),
                            queryEntry.getValue(), nodePath);

                    if (matches) {
                        if (!containsNode) {
                            fireQueryChildAdded(nodePath, parent, value);
                            queryEvaluator.addNodeToQuery(nodePath.parent(), queryEntry.getValue(), nodePath);
                        } else {
                            fireQueryChildChanged(nodePath, parent, value);
                        }
                    } else if (containsNode) {
                        fireQueryChildRemoved(nodePath, value);
                        queryEvaluator.removeNodeFromQuery(nodePath.parent(), queryEntry.getValue(),
                                nodePath);
                    }
                } else {
                    fireQueryChildRemoved(nodePath, null);
                    queryEvaluator.removeNodeFromQuery(nodePath.parent(), queryEntry.getValue(), nodePath);
                }
            }
        }
    }

    public void fireQueryChildAdded(Path path, JsonObject parent, Object value) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(value)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                JsonObject broadcast = new JsonObject();
                broadcast.putValue(HeliumEvent.TYPE, QUERY_CHILD_ADDED);
                broadcast.putValue("name", path.lastElement());
                broadcast.putValue(HeliumEvent.PATH, createPath(path.parent()));
                broadcast.putValue("parent", createPath(path.parent().parent()));
                broadcast.putValue(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, value));
                broadcast.putValue("hasChildren", MapDbBackedNode.hasChildren(value));
                broadcast.putValue("numChildren", MapDbBackedNode.childCount(value));
                sendViaWebsocket(broadcast);
            }
        });
    }

    private void sendViaWebsocket(JsonObject broadcast) {
        socket.write(new Buffer(broadcast.toString()));
    }

    public void fireChildChanged(String name, Path path, Path parent, Object node,
                                 boolean hasChildren, long numChildren) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(node)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                JsonObject broadcast = new JsonObject();
                broadcast.putValue(HeliumEvent.TYPE, CHILD_CHANGED);
                broadcast.putValue("name", name);
                broadcast.putValue(HeliumEvent.PATH, createPath(path));
                broadcast.putValue("parent", createPath(parent));
                broadcast.putValue(HeliumEvent.PAYLOAD, MapDbBackedNode.filterContent(auth, path, node));
                broadcast.putValue("hasChildren", hasChildren);
                broadcast.putValue("numChildren", numChildren);
                sendViaWebsocket(broadcast);
            }
        });
    }

    public void fireChildRemoved(Path path, String name, Object payload) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(payload)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                JsonObject broadcast = new JsonObject();
                broadcast.putValue(HeliumEvent.TYPE, CHILD_REMOVED);
                broadcast.putValue(HeliumEvent.NAME, name);
                broadcast.putValue(HeliumEvent.PATH, createPath(path));
                broadcast.putValue(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, payload));
                sendViaWebsocket(broadcast);
            }
        });
    }

    @Override
    public void fireValue(String name, Path path, Path parent, Object value) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(value)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                JsonObject broadcast = new JsonObject();
                broadcast.putValue(HeliumEvent.TYPE, VALUE);
                broadcast.putValue("name", name);
                broadcast.putValue(HeliumEvent.PATH, createPath(path));
                broadcast.putValue("parent", createPath(parent));
                broadcast.putValue(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, value));
                sendViaWebsocket(broadcast);
            }
        });
    }

    @Override
    public void fireChildAdded(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren) {

        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(value)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                JsonObject broadcast = new JsonObject();
                broadcast.putValue(HeliumEvent.TYPE, CHILD_ADDED);
                broadcast.putValue("name", name);
                broadcast.putValue(HeliumEvent.PATH, createPath(path));
                broadcast.putValue("parent", createPath(parent));
                broadcast.putValue(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, value));
                broadcast.putValue("hasChildren", hasChildren);
                broadcast.putValue("numChildren", numChildren);
                sendViaWebsocket(broadcast);
            }
        });
    }

    public void fireQueryChildChanged(Path path, JsonObject parent, Object value) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(value)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                JsonObject broadcast = new JsonObject();
                broadcast.putValue(HeliumEvent.TYPE, QUERY_CHILD_CHANGED);
                broadcast.putValue("name", path.lastElement());
                broadcast.putValue(HeliumEvent.PATH, createPath(path.parent()));
                broadcast.putValue("parent", createPath(path.parent().parent()));
                broadcast.putValue(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, value));
                broadcast.putValue("hasChildren", MapDbBackedNode.hasChildren(value));
                broadcast.putValue("numChildren", MapDbBackedNode.childCount(value));
                sendViaWebsocket(broadcast);
            }
        });
    }

    public void fireQueryChildRemoved(Path path, Object payload) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(payload)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                JsonObject broadcast = new JsonObject();
                broadcast.putValue(HeliumEvent.TYPE, QUERY_CHILD_REMOVED);
                broadcast.putValue(HeliumEvent.NAME, path.lastElement());
                broadcast.putValue(HeliumEvent.PATH, createPath(path.parent()));
                broadcast.putValue(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, payload));
                sendViaWebsocket(broadcast);
            }
        });
    }

    public void distributeEvent(Path path, JsonObject payload) {
        if (hasListener(path, "event")) {
            vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(payload)), (Handler<Message<Boolean>>) event -> {
                if (event.body()) {
                    JsonObject broadcast = new JsonObject();
                    broadcast.putValue(HeliumEvent.TYPE, "event");

                    broadcast.putValue(HeliumEvent.PATH, createPath(path));
                    broadcast.putValue(HeliumEvent.PAYLOAD, payload);
                    LOGGER.info("Distributing Message (basePath: '" + basePath + "',path: '" + path + "') : "
                            + broadcast.toString());
                    sendViaWebsocket(broadcast);
                }
            });
        }
    }

    private String createPath(String path) {
        if (basePath.endsWith("/") && path.startsWith("/")) {
            return basePath + path.substring(1);
        } else {
            return basePath + path;
        }
    }

    private String createPath(Path path) {
        return createPath(path.toString());
    }

    public void addListener(Path path, String type) {
        attached_listeners.put(path.toString(), type);
    }

    public void removeListener(Path path, String type) {
        attached_listeners.remove(path, type);
    }

    private boolean hasListener(Path path, String type) {
        if (path.isEmtpy()) {
            return attached_listeners.containsKey("/") && attached_listeners.get("/").contains(type);
        } else {
            return attached_listeners.containsKey(path.toString())
                    && attached_listeners.get(path.toString()).contains(type);
        }
    }

    public void addQuery(Path path, String query) {
        queryEvaluator.addQuery(path, query);
    }

    public void removeQuery(Path path, String query) {
        queryEvaluator.removeQuery(path, query);
    }

    public boolean hasQuery(Path path) {
        return queryEvaluator.hasQuery(path);
    }

    public void registerDisconnectEvent(HeliumEvent heliumEvent) {
        disconnectEvents.add(heliumEvent.copy());
    }

    public void executeDisconnectEvents() {
        for (HeliumEvent event : disconnectEvents) {
            vertx.eventBus().send(Distributor.DISTRIBUTE_HELIUM_EVENT, event);
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void send(String msg) {
        socket.write(new Buffer(msg));
    }

    public void syncPath(ChangeLog log, Path path, Endpoint handler) {
        MapDbBackedNode node;
        if (MapDbBackedNode.exists(path)) {
            node = MapDbBackedNode.of(path);
        } else {
            node = MapDbBackedNode.of(path.parent());
        }

        for (String childNodeKey : node.keys()) {
            Object object = node.get(childNodeKey);
            boolean hasChildren = (object instanceof MapDbBackedNode) ? ((MapDbBackedNode) object).hasChildren() : false;
            int indexOf = node.indexOf(childNodeKey);
            int numChildren = (object instanceof MapDbBackedNode) ? ((MapDbBackedNode) object).length() : 0;
            if (object != null && object != null) {
                handler.fireChildAdded(childNodeKey, path, path.parent(), object, hasChildren,
                        numChildren);
            }
        }
    }


    public void syncPathWithQuery(ChangeLog log, Path path, WebsocketEndpoint handler,
                                  QueryEvaluator queryEvaluator, String query) {
        MapDbBackedNode node;
        if (MapDbBackedNode.exists(path)) {
            node = MapDbBackedNode.of(path);
        } else {
            node = MapDbBackedNode.of(path.parent());
        }

        for (String childNodeKey : node.keys()) {
            Object object = node.get(childNodeKey);
            if (queryEvaluator.evaluateQueryOnValue(object, query)) {
                if (object != null && object != null) {
                    handler.fireQueryChildAdded(path, node.toJsonObject(), object);
                }
            }
        }
    }

    public void syncPropertyValue(ChangeLog log, Path path, Endpoint handler) {
        MapDbBackedNode node = MapDbBackedNode.of(path.parent());
        String childNodeKey = path.lastElement();
        if (node.has(path.lastElement())) {
            Object object = node.get(path.lastElement());
            handler.fireValue(childNodeKey, path, path.parent(), object
            );
        } else {
            handler.fireValue(childNodeKey, path, path.parent(), "");
        }
    }

}


