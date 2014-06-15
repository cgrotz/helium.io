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

package io.helium.server.channels.websocket;

import com.google.common.base.Strings;
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
import io.helium.persistence.EventSource;
import io.helium.persistence.Persistor;
import io.helium.persistence.infinispan.Node;
import io.helium.persistence.queries.QueryEvaluator;
import io.helium.server.Endpoint;
import io.helium.server.channels.websocket.rpc.Rpc;
import io.helium.server.distributor.Distributor;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

public class WebsocketEndpoint implements Endpoint {
    private final Vertx vertx;
    private Multimap<String, String> attached_listeners = HashMultimap.create();
    private String basePath;
    private Optional<JsonObject> auth = Optional.empty();
    private QueryEvaluator queryEvaluator;
    private ServerWebSocket socket;
    private List<HeliumEvent> disconnectEvents = Lists.newArrayList();

    private boolean open = true;

    private Rpc rpc;
    private final Container container;

    public WebsocketEndpoint(String basePath,
                             ServerWebSocket socket,
                             Vertx vertx,
                             Container container) {
        this.socket = socket;
        this.container = container;
        this.vertx = vertx;
        this.basePath = basePath;
        this.queryEvaluator = new QueryEvaluator(container);

        this.rpc = new Rpc(container);
        this.rpc.register(this);

        this.socket.dataHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                rpc.handle(event.toString(), WebsocketEndpoint.this);
            }
        });
    }


    @Rpc.Method
    public void attachListener(@Rpc.Param("path") String path,
                               @Rpc.Param("event_type") String eventType) {
        container.logger().trace("attachListener");
        addListener(new Path(HeliumEvent.extractPath(path)), eventType);
        if ("child_added".equals(eventType)) {
            ChangeLog log = ChangeLog.of(new JsonArray());
            syncPath(log, Path.of(HeliumEvent.extractPath(path)), this);
            distributeChangeLog(log);
        } else if ("value".equals(eventType)) {
            ChangeLog log = ChangeLog.of(new JsonArray());
            syncPropertyValue(log, new Path(HeliumEvent.extractPath(path)), this);
            distributeChangeLog(log);
        }
    }

    @Rpc.Method
    public void detachListener(@Rpc.Param("path") String path,
                               @Rpc.Param("event_type") String eventType) {
        container.logger().trace("detachListener");
        removeListener(new Path(HeliumEvent.extractPath(path)), eventType);
    }

    @Rpc.Method
    public void attachQuery(@Rpc.Param("path") String path, @Rpc.Param("query") String query) {
        container.logger().trace("attachQuery");
        addQuery(new Path(HeliumEvent.extractPath(path)), query);
        syncPathWithQuery(ChangeLog.of(new JsonArray()), new Path(HeliumEvent.extractPath(path)), this,
                new QueryEvaluator(container), query);
    }

    @Rpc.Method
    public void detachQuery(@Rpc.Param("path") String path, @Rpc.Param("query") String query) {
        container.logger().trace("detachQuery");
        removeQuery(new Path(HeliumEvent.extractPath(path)), query);
    }

    @Rpc.Method
    public void event(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        container.logger().trace("event");
        vertx.eventBus().send(Distributor.DISTRIBUTE_EVENT, new JsonObject().putString("path", path).putObject("payload", data));
    }

    @Rpc.Method
    public void push(@Rpc.Param("path") String path, @Rpc.Param("name") String name,
                     @Rpc.Param("data") JsonObject data) {
        container.logger().trace("push");
        HeliumEvent event = new HeliumEvent(HeliumEventType.PUSH, path + "/" + name, data);
        if (auth.isPresent())
            event.setAuth(auth.get());

        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.WRITE, auth, Path.of(path), data), (Message<Boolean> event1) -> {
            if (event1.body()) {
                vertx.eventBus().send(EventSource.PERSIST_EVENT, event);
                vertx.eventBus().send(Persistor.SUBSCRIPTION_PUSH, event);
                container.logger().trace("authorized: " + event);
            } else {
                container.logger().warn("not authorized: " + event);
            }
        });
    }

    @Rpc.Method
    public void set(@Rpc.Param("path") String path, @Rpc.Param("data") Object data) {
        container.logger().trace("set");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SET, path, data);
        if (auth.isPresent())
            event.setAuth(auth.get());

        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.WRITE, auth, Path.of(path), data), (Message<Boolean> event1) -> {
            if (event1.body()) {
                vertx.eventBus().send(EventSource.PERSIST_EVENT, event);
                vertx.eventBus().send(Persistor.SUBSCRIPTION_SET, event);
                container.logger().trace("authorized: " + event);
            } else {
                container.logger().warn("not authorized: " + event);
            }
        });
    }

    @Rpc.Method
    public void update(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        container.logger().trace("update");
        HeliumEvent event = new HeliumEvent(HeliumEventType.UPDATE, path, data);
        if (auth.isPresent())
            event.setAuth(auth.get());

        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.WRITE, auth, Path.of(path), null), (Message<Boolean> event1) -> {
            if (event1.body()) {
                vertx.eventBus().send(EventSource.PERSIST_EVENT, event);
                vertx.eventBus().send(Persistor.SUBSCRIPTION_UPDATE, event);
                container.logger().trace("authorized: " + event);
            } else {
                container.logger().warn("not authorized: " + event);
            }
        });
    }

    @Rpc.Method
    public void pushOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("name") String name,
                                 @Rpc.Param("payload") JsonObject payload) {
        container.logger().trace("pushOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.PUSH, path + "/" + name,
                payload);
        if (auth.isPresent())
            event.setAuth(auth.get());
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void setOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        container.logger().trace("setOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SET, path, data);
        if (auth.isPresent())
            event.setAuth(auth.get());
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void updateOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        container.logger().trace("updateOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.UPDATE, path, data);
        if (auth.isPresent())
            event.setAuth(auth.get());
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void removeOnDisconnect(@Rpc.Param("path") String path) {
        container.logger().trace("removeOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.REMOVE, path);
        if (auth.isPresent())
            event.setAuth(auth.get());
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void authenticate(@Rpc.Param("username") String username, @Rpc.Param("password") String password) {
        container.logger().trace("authenticate");
        extractAuthentication(username, password, new Handler<Optional<JsonObject>>() {
            @Override
            public void handle(Optional<JsonObject> event) {
                auth = event;
                for (String path : attached_listeners.keys()) {
                    for (String eventType : attached_listeners.get(path))
                        if ("child_added".equals(eventType)) {
                            ChangeLog log = ChangeLog.of(new JsonArray());
                            syncPath(log, Path.of(HeliumEvent.extractPath(path)), WebsocketEndpoint.this);
                            distributeChangeLog(log);
                        } else if ("value".equals(eventType)) {
                            ChangeLog log = ChangeLog.of(new JsonArray());
                            syncPropertyValue(log, new Path(HeliumEvent.extractPath(path)), WebsocketEndpoint.this);
                            distributeChangeLog(log);
                        }
                }
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
            container.logger().trace("distribute " + (System.currentTimeMillis() - startTime) + "ms; event processing time " + (System.currentTimeMillis() - event.getLong("creationDate")) + "ms");
        }
    }

    public void distributeChangeLog(ChangeLog changeLog) {
        changeLog.forEach(obj -> {
            JsonObject logE = (JsonObject) obj;

            if (logE.getString("type").equals(ChildAddedLogEvent.class.getSimpleName())) {
                ChildAddedLogEvent logEvent = ChildAddedLogEvent.of(logE);
                if (hasListener(logEvent.getPath(), CHILD_ADDED)) {
                    fireChildAdded(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren()
                    );
                }
            }
            if (logE.getString("type").equals(ChildChangedLogEvent.class.getSimpleName())) {
                ChildChangedLogEvent logEvent = ChildChangedLogEvent.of(logE);
                if (hasListener(logEvent.getPath(), CHILD_CHANGED)) {
                    fireChildChanged(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren()
                    );
                }
            }
            if (logE.getString("type").equals(ValueChangedLogEvent.class.getSimpleName())) {
                ValueChangedLogEvent logEvent = ValueChangedLogEvent.of(logE);
                if (hasListener(logEvent.getPath(), VALUE)) {
                    fireValue(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue());
                }
            }
            if (logE.getString("type").equals(ChildRemovedLogEvent.class.getSimpleName())) {
                ChildRemovedLogEvent logEvent = ChildRemovedLogEvent.of(logE);
                if (hasListener(logEvent.getPath(), CHILD_REMOVED)) {
                    fireChildRemoved(logEvent.getPath(), logEvent.getName(), logEvent.getValue());
                }
            }
        });
    }

    private void processQuery(HeliumEvent event) {
        Path nodePath = event.extractNodePath();
        if (Node.exists(nodePath)) {
            nodePath = nodePath.parent();
        }

        if (hasQuery(nodePath.parent())) {
            for (Entry<String, String> queryEntry : queryEvaluator.getQueries()) {
                if (event.getPayload() != null) {
                    JsonObject value = Node.of(nodePath).toJsonObject();
                    JsonObject parent = Node.of(nodePath.parent()).toJsonObject();
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
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, value), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                vertx.eventBus().send(Authorizator.FILTER_CONTENT, Authorizator.filter(auth, path, value), new Handler<Message<Object>>() {
                    @Override
                    public void handle(Message<Object> event) {
                        JsonObject broadcast = new JsonObject();
                        broadcast.putValue(HeliumEvent.TYPE, QUERY_CHILD_ADDED);
                        broadcast.putValue("name", path.lastElement());
                        broadcast.putValue(HeliumEvent.PATH, createPath(path.parent()));
                        broadcast.putValue("parent", createPath(path.parent().parent()));
                        broadcast.putValue(HeliumEvent.PAYLOAD, event.body());
                        broadcast.putValue("hasChildren", Node.hasChildren(value));
                        broadcast.putValue("numChildren", Node.childCount(value));
                        sendViaWebsocket(broadcast);
                    }
                });
            }
        });
    }

    private void sendViaWebsocket(JsonObject broadcast) {
        try {
            socket.writeTextFrame(broadcast.toString());
        } catch (IllegalStateException e) {
            // Websocket was probably closed
            socket.close();
        }
    }

    public void fireChildChanged(String name, Path path, Path parent, Object node,
                                 boolean hasChildren, long numChildren) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(node)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                vertx.eventBus().send(Authorizator.FILTER_CONTENT, Authorizator.filter(auth, path, node), new Handler<Message<Object>>() {
                    @Override
                    public void handle(Message<Object> event) {
                        JsonObject broadcast = new JsonObject();
                        broadcast.putValue(HeliumEvent.TYPE, CHILD_CHANGED);
                        broadcast.putValue("name", name);
                        broadcast.putValue(HeliumEvent.PATH, createPath(path));
                        broadcast.putValue("parent", createPath(parent));
                        broadcast.putValue(HeliumEvent.PAYLOAD, event.body());
                        broadcast.putValue("hasChildren", hasChildren);
                        broadcast.putValue("numChildren", numChildren);
                        sendViaWebsocket(broadcast);
                    }
                });
            }
        });
    }

    public void fireChildRemoved(Path path, String name, Object payload) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(payload)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                vertx.eventBus().send(Authorizator.FILTER_CONTENT, Authorizator.filter(auth, path, payload), new Handler<Message<Object>>() {
                    @Override
                    public void handle(Message<Object> event) {
                        JsonObject broadcast = new JsonObject();
                        broadcast.putValue(HeliumEvent.TYPE, CHILD_REMOVED);
                        broadcast.putValue(HeliumEvent.NAME, name);
                        broadcast.putValue(HeliumEvent.PATH, createPath(path));
                        broadcast.putValue(HeliumEvent.PAYLOAD, event.body());
                        sendViaWebsocket(broadcast);
                    }
                });
            }
        });
    }

    @Override
    public void fireValue(String name, Path path, Path parent, Object value) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, value), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                vertx.eventBus().send(Authorizator.FILTER_CONTENT, Authorizator.filter(auth, path, value), new Handler<Message<Object>>() {
                    @Override
                    public void handle(Message<Object> event) {
                        JsonObject broadcast = new JsonObject();
                        broadcast.putValue(HeliumEvent.TYPE, VALUE);
                        broadcast.putValue("name", name);
                        broadcast.putValue(HeliumEvent.PATH, createPath(path));
                        broadcast.putValue("parent", createPath(parent));
                        broadcast.putValue(HeliumEvent.PAYLOAD, event.body());
                        sendViaWebsocket(broadcast);
                    }
                });
            }
        });
    }

    @Override
    public void fireChildAdded(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren) {

        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, value), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                vertx.eventBus().send(Authorizator.FILTER_CONTENT, Authorizator.filter(auth, path, value), new Handler<Message<Object>>() {
                    @Override
                    public void handle(Message<Object> event) {
                        JsonObject broadcast = new JsonObject();
                        broadcast.putValue(HeliumEvent.TYPE, CHILD_ADDED);
                        broadcast.putValue("name", name);
                        broadcast.putValue(HeliumEvent.PATH, createPath(path));
                        broadcast.putValue("parent", createPath(parent));
                        broadcast.putValue(HeliumEvent.PAYLOAD, event.body());
                        broadcast.putValue("hasChildren", hasChildren);
                        broadcast.putValue("numChildren", numChildren);
                        sendViaWebsocket(broadcast);
                    }
                });
            }
        });
    }

    public void fireQueryChildChanged(Path path, JsonObject parent, Object value) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, value), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                vertx.eventBus().send(Authorizator.FILTER_CONTENT, Authorizator.filter(auth, path, value), new Handler<Message<Object>>() {
                    @Override
                    public void handle(Message<Object> event) {
                        JsonObject broadcast = new JsonObject();
                        broadcast.putValue(HeliumEvent.TYPE, QUERY_CHILD_CHANGED);
                        broadcast.putValue("name", path.lastElement());
                        broadcast.putValue(HeliumEvent.PATH, createPath(path.parent()));
                        broadcast.putValue("parent", createPath(path.parent().parent()));
                        broadcast.putValue(HeliumEvent.PAYLOAD, event.body());
                        broadcast.putValue("hasChildren", Node.hasChildren(value));
                        broadcast.putValue("numChildren", Node.childCount(value));
                        sendViaWebsocket(broadcast);
                    }
                });
            }
        });
    }

    public void fireQueryChildRemoved(Path path, Object payload) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, new DataSnapshot(payload)), (Handler<Message<Boolean>>) event -> {
            if (event.body()) {
                vertx.eventBus().send(Authorizator.FILTER_CONTENT, Authorizator.filter(auth, path, payload), new Handler<Message<Object>>() {
                    @Override
                    public void handle(Message<Object> event) {
                        JsonObject broadcast = new JsonObject();
                        broadcast.putValue(HeliumEvent.TYPE, QUERY_CHILD_REMOVED);
                        broadcast.putValue(HeliumEvent.NAME, path.lastElement());
                        broadcast.putValue(HeliumEvent.PATH, createPath(path.parent()));
                        broadcast.putValue(HeliumEvent.PAYLOAD, event.body());
                        sendViaWebsocket(broadcast);
                    }
                });
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
                    container.logger().trace("Distributing Message (basePath: '" + basePath + "',path: '" + path + "') : "
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
            vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.WRITE, auth, Path.of(event.getPath()), event.getPayload()), (Message<Boolean> event1) -> {
                if (event1.body()) {
                    vertx.eventBus().send(EventSource.PERSIST_EVENT, event);
                    vertx.eventBus().send(event.getType().eventBus, event);
                    container.logger().trace("authorized: " + event);
                } else {
                    container.logger().warn("not authorized: " + event);
                }
            });
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void send(String msg) {
        if (!Strings.isNullOrEmpty(msg)) {
            socket.write(new Buffer(msg));
        }
    }

    public void syncPath(ChangeLog log, Path path, Endpoint handler) {
        Node node;
        if (Node.exists(path)) {
            node = Node.of(path);
        } else {
            node = Node.of(path.parent());
        }

        for (String childNodeKey : node.keys()) {
            if (!Strings.isNullOrEmpty(childNodeKey)) {
                Object object = node.get(childNodeKey);
                boolean hasChildren = (object instanceof Node) ? ((Node) object).hasChildren() : false;
                int numChildren = (object instanceof Node) ? ((Node) object).length() : 0;
                if (object != null && object != null) {
                    handler.fireChildAdded(childNodeKey, path, path.parent(), object, hasChildren,
                            numChildren);
                }
            }
        }
    }


    public void syncPathWithQuery(ChangeLog log, Path path, WebsocketEndpoint handler,
                                  QueryEvaluator queryEvaluator, String query) {
        Node node;
        if (Node.exists(path)) {
            node = Node.of(path);
        } else {
            node = Node.of(path.parent());
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
        Node node = Node.of(path.parent());
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


