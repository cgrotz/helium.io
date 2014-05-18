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

package io.helium.messaging;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import io.helium.Helium;
import io.helium.authorization.Authorization;
import io.helium.authorization.HeliumOperation;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.changelog.*;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;
import io.helium.persistence.inmemory.InMemoryPersistence;
import io.helium.queries.QueryEvaluator;
import io.helium.rpc.Rpc;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map.Entry;

public class HeliumEndpoint implements HeliumEventDistributor, HeliumOutboundSocket {
    private static final String QUERY_CHILD_REMOVED = "query_child_removed";

    private static final String QUERY_CHILD_CHANGED = "query_child_changed";

    private static final String QUERY_CHILD_ADDED = "query_child_added";

    private static final String CHILD_REMOVED = "child_removed";

    private static final String CHILD_MOVED = "child_moved";

    private static final String VALUE = "value";

    private static final String CHILD_CHANGED = "child_changed";

    private static final String CHILD_ADDED = "child_added";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(HeliumEndpoint.class);
    private final Channel channel;

    private Multimap<String, String> attached_listeners = HashMultimap.create();
    private String basePath;
    private Node auth;
    private Authorization authorization;
    private Persistence persistence;
    private QueryEvaluator queryEvaluator;

    private List<HeliumEvent> disconnectEvents = Lists.newArrayList();

    private Helium helium;

    private boolean open = true;

    private Rpc rpc;

    public HeliumEndpoint(String basePath, Node auth, Channel channel,
                          Persistence persistence, Authorization authorization, Helium helium) {
        this.channel = channel;
        this.persistence = persistence;
        this.authorization = authorization;
        this.auth = auth;
        this.basePath = basePath;
        this.queryEvaluator = new QueryEvaluator();
        this.helium = helium;

        this.rpc = new Rpc();
        this.rpc.register(this);
    }

    @Override
    public void distribute(HeliumEvent event) {
        if (open) {
            if (event.getType() == HeliumEventType.EVENT) {
                Node jsonObject;
                Object object = event.get(HeliumEvent.PAYLOAD);
                if (object instanceof Node) {
                    jsonObject = event.getNode(HeliumEvent.PAYLOAD);
                    distributeEvent(event.extractNodePath(), jsonObject);
                } else if (object instanceof String) {
                    jsonObject = new Node(HeliumEvent.PAYLOAD);
                    distributeEvent(event.extractNodePath(), new Node((String) object));
                }
            } else {
                processQuery(event);
                ChangeLog changeLog = event.getChangeLog();
                distributeChangeLog(changeLog);
            }
        }
    }

    public void distributeChangeLog(ChangeLog changeLog) {
        for (ChangeLogEvent logE : changeLog.getLog()) {
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
        }
    }

    private void processQuery(HeliumEvent event) {
        Path nodePath = event.extractNodePath();
        if (!(persistence.get(nodePath) instanceof Node)) {
            nodePath = nodePath.getParent();
        }

        if (hasQuery(nodePath.getParent())) {
            for (Entry<String, String> queryEntry : queryEvaluator.getQueries()) {
                if (event.getPayload() != null) {
                    Node value = persistence.getNode(nodePath);
                    Node parent = persistence.getNode(nodePath.getParent());
                    boolean matches = queryEvaluator.evaluateQueryOnValue(value, queryEntry.getValue());
                    boolean containsNode = queryEvaluator.queryContainsNode(new Path(queryEntry.getKey()),
                            queryEntry.getValue(), nodePath);

                    if (matches) {
                        if (!containsNode) {
                            fireQueryChildAdded(nodePath, parent, value);
                            queryEvaluator.addNodeToQuery(nodePath.getParent(), queryEntry.getValue(), nodePath);
                        } else {
                            fireQueryChildChanged(nodePath, parent, value);
                        }
                    } else if (containsNode) {
                        fireQueryChildRemoved(nodePath, value);
                        queryEvaluator.removeNodeFromQuery(nodePath.getParent(), queryEntry.getValue(),
                                nodePath);
                    }
                } else {
                    fireQueryChildRemoved(nodePath, null);
                    queryEvaluator.removeNodeFromQuery(nodePath.getParent(), queryEntry.getValue(), nodePath);
                }
            }
        }
    }

    public void fireChildAdded(String name, Path path, Path parent, Object node, boolean hasChildren,
                               long numChildren, String prevChildName, int priority) {
        if (authorization.isAuthorized(HeliumOperation.READ, auth, persistence.getRoot(), path,
                new InMemoryDataSnapshot(node))) {
            Node broadcast = new Node();
            broadcast.put(HeliumEvent.TYPE, CHILD_ADDED);
            broadcast.put("name", name);
            broadcast.put(HeliumEvent.PATH, createPath(path));
            broadcast.put("parent", createPath(parent));
            broadcast.put(HeliumEvent.PAYLOAD, checkPayload(path, node));
            broadcast.put("hasChildren", hasChildren);
            broadcast.put("numChildren", numChildren);
            broadcast.put("priority", priority);
            channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
        }
    }

    public void fireChildChanged(String name, Path path, Path parent, Object node,
                                 boolean hasChildren, long numChildren, String prevChildName, int priority) {
        if (node != null && node != Node.NULL) {
            if (authorization.isAuthorized(HeliumOperation.READ, auth, persistence.getRoot(), path,
                    new InMemoryDataSnapshot(node))) {
                Node broadcast = new Node();
                broadcast.put(HeliumEvent.TYPE, CHILD_CHANGED);
                broadcast.put("name", name);
                broadcast.put(HeliumEvent.PATH, createPath(path));
                broadcast.put("parent", createPath(parent));
                broadcast.put(HeliumEvent.PAYLOAD, checkPayload(path, node));
                broadcast.put("hasChildren", hasChildren);
                broadcast.put("numChildren", numChildren);
                broadcast.put("priority", priority);
                channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
            }
        }
    }

    public void fireChildRemoved(Path path, String name, Object payload) {
        if (authorization.isAuthorized(HeliumOperation.READ, auth, persistence.getRoot(), path,
                new InMemoryDataSnapshot(payload))) {
            Node broadcast = new Node();
            broadcast.put(HeliumEvent.TYPE, CHILD_REMOVED);
            broadcast.put(HeliumEvent.NAME, name);
            broadcast.put(HeliumEvent.PATH, createPath(path));
            broadcast.put(HeliumEvent.PAYLOAD, checkPayload(path, payload));
            channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
        }
    }

    public void fireValue(String name, Path path, Path parent, Object value, String prevChildName,
                          int priority) {
        if (authorization.isAuthorized(HeliumOperation.READ, auth, persistence.getRoot(), path,
                new InMemoryDataSnapshot(value))) {
            Node broadcast = new Node();
            broadcast.put(HeliumEvent.TYPE, VALUE);
            broadcast.put("name", name);
            broadcast.put(HeliumEvent.PATH, createPath(path));
            broadcast.put("parent", createPath(parent));
            broadcast.put(HeliumEvent.PAYLOAD, checkPayload(path, value));
            broadcast.put("priority", priority);
            channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
        }
    }

    public void fireChildMoved(Node childSnapshot, boolean hasChildren, long numChildren) {
        Node broadcast = new Node();
        broadcast.put(HeliumEvent.TYPE, CHILD_MOVED);
        broadcast.put(HeliumEvent.PAYLOAD, childSnapshot);
        broadcast.put("hasChildren", hasChildren);
        broadcast.put("numChildren", numChildren);
        channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
    }

    public void fireQueryChildAdded(Path path, Node parent, Object value) {
        if (authorization.isAuthorized(HeliumOperation.READ, auth, persistence.getRoot(), path,
                new InMemoryDataSnapshot(value))) {
            Node broadcast = new Node();
            broadcast.put(HeliumEvent.TYPE, QUERY_CHILD_ADDED);
            broadcast.put("name", path.getLastElement());
            broadcast.put(HeliumEvent.PATH, createPath(path.getParent()));
            broadcast.put("parent", createPath(path.getParent().getParent()));
            broadcast.put(HeliumEvent.PAYLOAD, checkPayload(path, value));
            broadcast.put("hasChildren", InMemoryPersistence.hasChildren(value));
            broadcast.put("numChildren", InMemoryPersistence.childCount(value));
            broadcast.put("priority", InMemoryPersistence.priority(parent, path.getLastElement()));
            channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
        }
    }

    public void fireQueryChildChanged(Path path, Node parent, Object value) {
        if (value != null && value != Node.NULL) {
            if (authorization.isAuthorized(HeliumOperation.READ, auth, persistence.getRoot(), path,
                    new InMemoryDataSnapshot(value))) {
                Node broadcast = new Node();
                broadcast.put(HeliumEvent.TYPE, QUERY_CHILD_CHANGED);
                broadcast.put("name", path.getLastElement());
                broadcast.put(HeliumEvent.PATH, createPath(path.getParent()));
                broadcast.put("parent", createPath(path.getParent().getParent()));
                broadcast.put(HeliumEvent.PAYLOAD, checkPayload(path, value));
                broadcast.put("hasChildren", InMemoryPersistence.hasChildren(value));
                broadcast.put("numChildren", InMemoryPersistence.childCount(value));
                broadcast.put("priority", InMemoryPersistence.priority(parent, path.getLastElement()));
                channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
            }
        }
    }

    public void fireQueryChildRemoved(Path path, Object payload) {
        if (authorization.isAuthorized(HeliumOperation.READ, auth, persistence.getRoot(), path,
                new InMemoryDataSnapshot(payload))) {
            Node broadcast = new Node();
            broadcast.put(HeliumEvent.TYPE, QUERY_CHILD_REMOVED);
            broadcast.put(HeliumEvent.NAME, path.getLastElement());
            broadcast.put(HeliumEvent.PATH, createPath(path.getParent()));
            broadcast.put(HeliumEvent.PAYLOAD, checkPayload(path, payload));
            channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
        }
    }

    @Override
    public void distributeEvent(Path path, Node payload) {
        if (hasListener(path, "event")) {
            Node broadcast = new Node();
            broadcast.put(HeliumEvent.TYPE, "event");

            broadcast.put(HeliumEvent.PATH, createPath(path));
            broadcast.put(HeliumEvent.PAYLOAD, payload);
            LOGGER.trace("Distributing Message (basePath: '" + basePath + "',path: '" + path + "') : "
                    + broadcast.toString());
            channel.writeAndFlush(new TextWebSocketFrame(broadcast.toString()));
        }
    }

    private Object checkPayload(Path path, Object value) {
        if (value instanceof Node) {
            Node org = (Node) value;
            Node node = new Node();
            for (String key : org.keys()) {
                if (authorization.isAuthorized(HeliumOperation.READ, auth, persistence.getRoot(),
                        path.append(key), new InMemoryDataSnapshot(org.get(key)))) {
                    node.put(key, checkPayload(path.append(key), org.get(key)));
                }
            }
            return node;
        } else {
            return value;
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
            helium.handle(event);
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public void send(String msg) {
        channel.writeAndFlush(new TextWebSocketFrame(msg));
    }

    @Rpc.Method
    public void attachListener(@Rpc.Param("path") String path,
                               @Rpc.Param("event_type") String eventType) {
        LOGGER.trace("attachListener");
        addListener(new Path(HeliumEvent.extractPath(path)), eventType);
        if ("child_added".equals(eventType)) {
            this.persistence.syncPath(new Path(HeliumEvent.extractPath(path)), this);
        } else if ("value".equals(eventType)) {
            this.persistence.syncPropertyValue(new Path(HeliumEvent.extractPath(path)), this);
        }
    }

    @Rpc.Method
    public void detachListener(@Rpc.Param("path") String path,
                               @Rpc.Param("event_type") String eventType) {
        LOGGER.trace("detachListener");
        removeListener(new Path(HeliumEvent.extractPath(path)), eventType);
    }

    @Rpc.Method
    public void attachQuery(@Rpc.Param("path") String path, @Rpc.Param("query") String query) {
        LOGGER.trace("attachQuery");
        addQuery(new Path(HeliumEvent.extractPath(path)), query);
        this.persistence.syncPathWithQuery(new Path(HeliumEvent.extractPath(path)), this,
                new QueryEvaluator(), query);
    }

    @Rpc.Method
    public void detachQuery(@Rpc.Param("path") String path, @Rpc.Param("query") String query) {
        LOGGER.trace("detachQuery");
        removeQuery(new Path(HeliumEvent.extractPath(path)), query);
    }

    @Rpc.Method
    public void event(@Rpc.Param("path") String path, @Rpc.Param("data") Node data) {
        LOGGER.trace("event");
        this.helium.getDistributor().distribute(path, data);
    }

    @Rpc.Method
    public void push(@Rpc.Param("path") String path, @Rpc.Param("name") String name,
                     @Rpc.Param("data") Node data) {
        LOGGER.trace("push");
        HeliumEvent event = new HeliumEvent(HeliumEventType.PUSH, path + "/" + name, data);
        this.helium.handle(event);
    }

    @Rpc.Method
    public void set(@Rpc.Param("path") String path, @Rpc.Param("data") Object data,
                    @Rpc.Param(value = "priority", defaultValue = "-1") Integer priority) {
        LOGGER.trace("set");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SET, path, data, priority);
        this.helium.handle(event);
    }

    @Rpc.Method
    public void update(@Rpc.Param("path") String path, @Rpc.Param("data") Node data) {
        LOGGER.trace("update");
        HeliumEvent event = new HeliumEvent(HeliumEventType.UPDATE, path, data);
        this.helium.handle(event);
    }

    @Rpc.Method
    public void setPriority(@Rpc.Param("path") String path, @Rpc.Param("priority") Integer priority) {
        LOGGER.trace("setPriority");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SETPRIORITY, path, priority);
        this.helium.handle(event);
    }

    @Rpc.Method
    public void pushOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("name") String name,
                                 @Rpc.Param("payload") Node payload) {
        LOGGER.trace("pushOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.PUSH, path + "/" + name,
                payload);
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void setOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("data") Node data,
                                @Rpc.Param(value = "priority", defaultValue = "-1") Integer priority) {
        LOGGER.trace("setOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.SET, path, data, priority);
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void updateOnDisconnect(@Rpc.Param("path") String path, @Rpc.Param("data") Node data) {
        LOGGER.trace("updateOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.UPDATE, path, data);
        this.disconnectEvents.add(event);
    }

    @Rpc.Method
    public void removeOnDisconnect(@Rpc.Param("path") String path) {
        LOGGER.trace("removeOnDisconnect");
        HeliumEvent event = new HeliumEvent(HeliumEventType.REMOVE, path);
        this.disconnectEvents.add(event);
    }

    public void handle(String msg, Node auth) {
        rpc.handle(msg, this);
    }
}
