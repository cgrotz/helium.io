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
import io.helium.authorization.Operation;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.changelog.*;
import io.helium.persistence.DataSnapshot;
import io.helium.persistence.mapdb.MapDbBackedNode;
import io.helium.persistence.queries.QueryEvaluator;
import io.helium.server.Endpoint;
import io.helium.server.protocols.websocket.rpc.Rpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
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
        this.core.distribute(path, data);
    }

    @Rpc.Method
    public void push(@Rpc.Param("path") String path, @Rpc.Param("name") String name,
                     @Rpc.Param("data") JsonObject data) {
        LOGGER.info("push");
        HeliumEvent event = new HeliumEvent(HeliumEventType.PUSH, path + "/" + name, data);
        if(auth.isPresent())
            event.setAuth(auth.get());
        this.core.handle(event);
    }

    @Rpc.Method
    public void set(@Rpc.Param("path") String path, @Rpc.Param("data") Object data,
                    @Rpc.Param(value = "priority", defaultValue = "-1") Integer priority) {
        LOGGER.info("set");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SET, path, data, priority);
        if(auth.isPresent())
            event.setAuth(auth.get());
        this.core.handle(event);
    }

    @Rpc.Method
    public void update(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data) {
        LOGGER.info("update");
        HeliumEvent event = new HeliumEvent(HeliumEventType.UPDATE, path, data);
        if(auth.isPresent())
            event.setAuth(auth.get());
        this.core.handle(event);
    }

    @Rpc.Method
    public void setPriority(@Rpc.Param("path") String path, @Rpc.Param("priority") Integer priority) {
        LOGGER.info("setPriority");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SETPRIORITY, path, priority);
        if(auth.isPresent())
            event.setAuth(auth.get());
        this.core.handle(event);
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
    public void setOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("data") JsonObject data,
                                @Rpc.Param(value = "priority", defaultValue = "-1") Integer priority) {
        LOGGER.info("setOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SET, path, data, priority);
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
        auth = extractAuthentication(username, password);
    }

    private Optional<JsonObject> extractAuthentication(String username, String password) {
        Optional<JsonObject> auth = Optional.empty();
        JsonObject users = persistence.getNode(new ChangeLog(), new Path("/users"));
        for(Object value : users.values()) {
            if (value instanceof JsonObject) {
                JsonObject node = (JsonObject) value;
                String localUsername = node.getString("username");
                String localPassword = node.getString("password");
                if(username.equals(localUsername) && password.equals(localPassword)) {
                    auth = Optional.of(node);
                }
            }
        }
        return auth;
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
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren(),
                            logEvent.getPrevChildName(), logEvent.getPriority());
                }
            }
            if (logE instanceof ChildChangedLogEvent) {
                ChildChangedLogEvent logEvent = (ChildChangedLogEvent) logE;
                if (hasListener(logEvent.getPath(), CHILD_CHANGED)) {
                    fireChildChanged(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren(),
                            logEvent.getPrevChildName(), logEvent.getPriority());
                }
            }
            if (logE instanceof ValueChangedLogEvent) {
                ValueChangedLogEvent logEvent = (ValueChangedLogEvent) logE;
                if (hasListener(logEvent.getPath(), VALUE)) {
                    fireValue(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getPrevChildName(), logEvent.getPriority());
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
        if (!(persistence.get(nodePath) instanceof JsonObject)) {
            nodePath = nodePath.parent();
        }

        if (hasQuery(nodePath.parent())) {
            for (Entry<String, String> queryEntry : queryEvaluator.getQueries()) {
                if (event.getPayload() != null) {
                    JsonObject value = persistence.getNode(new ChangeLog(), nodePath);
                    JsonObject parent = persistence.getNode(new ChangeLog(), nodePath.parent());
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
        if (authorization.isAuthorized(Operation.READ, auth, path,
                new DataSnapshot(value))) {
            JsonObject broadcast = new JsonObject();
            broadcast.put(HeliumEvent.TYPE, QUERY_CHILD_ADDED);
            broadcast.put("name", path.lastElement());
            broadcast.put(HeliumEvent.PATH, createPath(path.parent()));
            broadcast.put("parent", createPath(path.parent().parent()));
            broadcast.put(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, value));
            broadcast.put("hasChildren", Persistence.hasChildren(value));
            broadcast.put("numChildren", Persistence.childCount(value));
            broadcast.put("priority", Persistence.priority(parent, path.lastElement()));
            sendViaWebsocket(broadcast);
        }
    }

    private void sendViaWebsocket(JsonObject broadcast) {
        socket.write(new Buffer(broadcast.toString()));
    }

    public void fireChildChanged(String name, Path path, Path parent, Object node,
                                 boolean hasChildren, long numChildren, String prevChildName, int priority) {
        if (node != null && node != JsonObject.NULL) {
            if (authorization.isAuthorized(Operation.READ, auth, path,
                    new DataSnapshot(node))) {
                JsonObject broadcast = new JsonObject();
                broadcast.put(HeliumEvent.TYPE, CHILD_CHANGED);
                broadcast.put("name", name);
                broadcast.put(HeliumEvent.PATH, createPath(path));
                broadcast.put("parent", createPath(parent));
                broadcast.put(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, node));
                broadcast.put("hasChildren", hasChildren);
                broadcast.put("numChildren", numChildren);
                broadcast.put("priority", priority);
                sendViaWebsocket(broadcast);
            }
        }
    }

    public void fireChildRemoved(Path path, String name, Object payload) {
        if (authorization.isAuthorized(Operation.READ, auth, path,
                new DataSnapshot(payload))) {
            JsonObject broadcast = new JsonObject();
            broadcast.put(HeliumEvent.TYPE, CHILD_REMOVED);
            broadcast.put(HeliumEvent.NAME, name);
            broadcast.put(HeliumEvent.PATH, createPath(path));
            broadcast.put(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, payload));
            sendViaWebsocket(broadcast);
        }
    }

    @Override
    public void fireValue(String name, Path path, Path parent, Object value, String prevChildName, int priority) {
        if (authorization.isAuthorized(Operation.READ, auth, path,
                new DataSnapshot(value))) {
            JsonObject broadcast = new JsonObject();
            broadcast.put(HeliumEvent.TYPE, VALUE);
            broadcast.put("name", name);
            broadcast.put(HeliumEvent.PATH, createPath(path));
            broadcast.put("parent", createPath(parent));
            broadcast.put(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, value));
            broadcast.put("priority", priority);
            sendViaWebsocket(broadcast);
        }
    }

    public void fireChildMoved(JsonObject childSnapshot, boolean hasChildren, long numChildren) {
        JsonObject broadcast = new JsonObject();
        broadcast.put(HeliumEvent.TYPE, CHILD_MOVED);
        broadcast.put(HeliumEvent.PAYLOAD, childSnapshot);
        broadcast.put("hasChildren", hasChildren);
        broadcast.put("numChildren", numChildren);
        sendViaWebsocket(broadcast);
    }

    @Override
    public void fireChildAdded(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren, String prevChildName, int priority) {
        if (authorization.isAuthorized(Operation.READ, auth, path,
                new DataSnapshot(value))) {
            JsonObject broadcast = new JsonObject();
            broadcast.put(HeliumEvent.TYPE, CHILD_ADDED);
            broadcast.put("name", name);
            broadcast.put(HeliumEvent.PATH, createPath(path));
            broadcast.put("parent", createPath(parent));
            broadcast.put(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, value));
            broadcast.put("hasChildren", hasChildren);
            broadcast.put("numChildren", numChildren);
            broadcast.put("priority", priority);
            sendViaWebsocket(broadcast);
        }
    }

    public void fireQueryChildChanged(Path path, JsonObject parent, Object value) {
        if (value != null && value != JsonObject.NULL) {
            if (authorization.isAuthorized(Operation.READ, auth, path,
                    new DataSnapshot(value))) {
                JsonObject broadcast = new JsonObject();
                broadcast.put(HeliumEvent.TYPE, QUERY_CHILD_CHANGED);
                broadcast.put("name", path.lastElement());
                broadcast.put(HeliumEvent.PATH, createPath(path.parent()));
                broadcast.put("parent", createPath(path.parent().parent()));
                broadcast.put(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, value));
                broadcast.put("hasChildren", Persistence.hasChildren(value));
                broadcast.put("numChildren", Persistence.childCount(value));
                broadcast.put("priority", Persistence.priority(parent, path.lastElement()));
                sendViaWebsocket(broadcast);
            }
        }
    }

    public void fireQueryChildRemoved(Path path, Object payload) {
        if (authorization.isAuthorized(Operation.READ,
                auth,
                path,
                new DataSnapshot(payload))) {
            JsonObject broadcast = new JsonObject();
            broadcast.put(HeliumEvent.TYPE, QUERY_CHILD_REMOVED);
            broadcast.put(HeliumEvent.NAME, path.lastElement());
            broadcast.put(HeliumEvent.PATH, createPath(path.parent()));
            broadcast.put(HeliumEvent.PAYLOAD, authorization.filterContent(auth, path, payload));
            sendViaWebsocket(broadcast);
        }
    }

    public void distributeEvent(Path path, JsonObject payload) {
        if (hasListener(path, "event")) {
            if (authorization.isAuthorized(Operation.READ,
                    auth,
                    path,
                    new DataSnapshot(payload))) {
                JsonObject broadcast = new JsonObject();
                broadcast.put(HeliumEvent.TYPE, "event");

                broadcast.put(HeliumEvent.PATH, createPath(path));
                broadcast.put(HeliumEvent.PAYLOAD, payload);
                LOGGER.info("Distributing Message (basePath: '" + basePath + "',path: '" + path + "') : "
                        + broadcast.toString());
                sendViaWebsocket(broadcast);
            }
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
            core.handle(event);
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
                        numChildren, null, indexOf);
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
                    handler.fireQueryChildAdded(path, node, object);
                }
            }
        }
    }

    public void syncPropertyValue(ChangeLog log, Path path, Endpoint handler) {
        MapDbBackedNode node = MapDbBackedNode.of(path.parent());
        String childNodeKey = path.lastElement();
        if (node.has(path.lastElement())) {
            Object object = node.get(path.lastElement());
            handler.fireValue(childNodeKey, path, path.parent(), object, "",
                    node.indexOf(childNodeKey));
        } else {
            handler.fireValue(childNodeKey, path, path.parent(), "", "", node.indexOf(childNodeKey));
        }
    }

}


