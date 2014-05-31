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

package io.helium.persistence.inmemory;

import io.helium.common.Path;
import io.helium.core.Core;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.json.NodeVisitor;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.queries.QueryEvaluator;
import io.helium.server.Endpoint;
import io.helium.server.protocols.websocket.WebsocketEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class InMemoryPersistence implements Persistence {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryPersistence.class);
    private Node model = new HashMapBackedNode();
    private Core core;
    private Authorization authorization;

    public static String prevChildName(Node parent, int priority) {
        if (priority <= 0) {
            return null;
        }
        return parent.keys().get(priority - 1);
    }

    public static long childCount(Object node) {
        return (node instanceof HashMapBackedNode) ? ((Node) node).getChildren().size() : 0;
    }

    public static int priority(Node parentNode, String name) {
        return parentNode.indexOf(name);
    }

    public static boolean hasChildren(Object node) {
        return (node instanceof HashMapBackedNode) ? ((Node) node).hasChildren() : false;
    }

    @Override
    public Object get(Path path) {
        if (path == null || path.isEmtpy() || model.getObjectForPath(path) == null) {
            return model;
        } else {
            return model.getObjectForPath(path);
        }
    }

    @Override
    public Node getNode(Path path) {
        ChangeLog log = new ChangeLog();
        Node nodeForPath = model.getNodeForPath(log, path);
        core.distributeChangeLog(log);
        return nodeForPath;
    }

    @Override
    public void remove(ChangeLog log, Optional<Node> auth, Path path) {
        String nodeName = path.getLastElement();
        Path parentPath = path.getParent();
        if(model.pathExists(path)) {
            Node node = model.getNodeForPath(log, parentPath).getNode(nodeName);

            if (authorization.isAuthorized(Operation.WRITE, auth,
                    new InMemoryDataSnapshot(model), path, node)) {
                Node parent = model.getNodeForPath(log, parentPath);
                node.accept(path, new ChildRemovedSubTreeVisitor(log));
                parent.remove(nodeName);
                log.addChildRemovedLogEntry(parentPath, nodeName, node);
                core.distributeChangeLog(log);
            }
        }
    }

    @Override
    public void syncPath(Path path, Endpoint handler) {

        ChangeLog log = new ChangeLog();
        Node node = model.getNodeForPath(log, path);

        for (String childNodeKey : node.keys()) {
            Object object = node.get(childNodeKey);
            boolean hasChildren = (object instanceof HashMapBackedNode) ? ((Node) object).hasChildren() : false;
            int indexOf = node.indexOf(childNodeKey);
            int numChildren = (object instanceof HashMapBackedNode) ? ((Node) object).length() : 0;
            if (object != null && object != HashMapBackedNode.NULL) {
                handler.fireChildAdded(childNodeKey, path, path.getParent(), object, hasChildren,
                        numChildren, null, indexOf);
            }
        }
        core.distributeChangeLog(log);

    }

    @Override
    public void syncPathWithQuery(Path path, WebsocketEndpoint handler,
                                  QueryEvaluator queryEvaluator, String query) {
        ChangeLog log = new ChangeLog();
        Node node = model.getNodeForPath(log, path);
        for (String childNodeKey : node.keys()) {
            Object object = node.get(childNodeKey);
            if (queryEvaluator.evaluateQueryOnValue(object, query)) {
                if (object != null && object != HashMapBackedNode.NULL) {
                    handler.fireQueryChildAdded(path, node, object);
                }
            }
        }
        core.distributeChangeLog(log);
    }

    @Override
    public void syncPropertyValue(Path path, Endpoint handler) {
        ChangeLog log = new ChangeLog();
        Node node = model.getNodeForPath(log, path.getParent());
        String childNodeKey = path.getLastElement();
        if (node.has(path.getLastElement())) {
            Object object = node.get(path.getLastElement());
            handler.fireValue(childNodeKey, path, path.getParent(), object, "",
                    node.indexOf(childNodeKey));
        } else {
            handler.fireValue(childNodeKey, path, path.getParent(), "", "", node.indexOf(childNodeKey));
        }
        core.distributeChangeLog(log);

    }

    @Override
    public void updateValue(ChangeLog log, Optional<Node> auth, Path path, int priority, Object payload) {
        Node node;
        boolean created = false;
        if (!model.pathExists(path)) {
            created = true;
        }
        Node parent = model.getNodeForPath(log, path.getParent());
        if (payload instanceof HashMapBackedNode) {
            if (parent.has(path.getLastElement())) {
                node = parent.getNode(path.getLastElement());
                parent.setIndexOf(path.getLastElement(), priority);
            } else {
                node = new HashMapBackedNode();
                parent.putWithIndex(path.getLastElement(), node, priority);
            }
            node.populate(new ChangeLogBuilder(log, path, path.getParent(), node), (Node) payload);
            if (created) {
                log.addChildAddedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
                                .getParent(), payload, false, 0,
                        prevChildName(parent, priority(parent, path.getLastElement())),
                        priority(parent, path.getLastElement())
                );
            } else {
                log.addChildChangedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
                                .getParent(), payload, false, 0,
                        prevChildName(parent, priority(parent, path.getLastElement())),
                        priority(parent, path.getLastElement())
                );
            }
        } else {
            parent.putWithIndex(path.getLastElement(), payload, priority);

            if (created) {
                log.addChildAddedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
                                .getParent(), payload, false, 0,
                        prevChildName(parent, priority(parent, path.getLastElement())),
                        priority(parent, path.getLastElement())
                );
            } else {
                log.addChildChangedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
                                .getParent(), payload, false, 0,
                        prevChildName(parent, priority(parent, path.getLastElement())),
                        priority(parent, path.getLastElement())
                );
                log.addValueChangedLogEntry(path.getLastElement(), path, path.getParent(), payload,
                        prevChildName(parent, priority(parent, path.getLastElement())),
                        priority(parent, path.getLastElement()));
            }
            log.addChildChangedLogEntry(path.getParent().getLastElement(), path.getParent().getParent(),
                    path.getParent().getParent().getParent(), parent, false, 0,
                    prevChildName(parent, priority(parent, path.getLastElement())),
                    priority(parent, path.getLastElement()));

        }
        logger.info("Model changed: " + model);
    }

    @Override
    public void applyNewValue(ChangeLog log, Optional<Node> auth, Path path, int priority, Object payload) {
        boolean created = false;
        if (!model.pathExists(path)) {
            created = true;
        }
        if (authorization.isAuthorized(Operation.WRITE, auth,
                new InMemoryDataSnapshot(model), path, payload)) {
            Node parent = model.getNodeForPath(log, path.getParent());
            if (payload instanceof HashMapBackedNode) {
                Node node = new HashMapBackedNode();
                populate(new ChangeLogBuilder(log, path, path.getParent(), node), path, auth, node,
                        (Node) payload);
                parent.putWithIndex(path.getLastElement(), node, priority);
            } else {
                parent.putWithIndex(path.getLastElement(), payload, priority);
            }

            if (created) {
                log.addChildAddedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
                                .getParent(), payload, false, 0,
                        prevChildName(parent, priority(parent, path.getLastElement())),
                        priority(parent, path.getLastElement())
                );
            } else {
                addChangeEvent(log, path);
            }
            {
                Path currentPath = path;
                while (!currentPath.isSimple()) {
                    log.addValueChangedLogEntry(currentPath.getLastElement(), currentPath,
                            currentPath.getParent(), model.getObjectForPath(currentPath), null, -1);
                    currentPath = currentPath.getParent();
                }
            }
        }
        logger.info("Model changed: " + model);
    }

    public void populate(ChangeLogBuilder logBuilder, Path path, Optional<Node> auth, Node node, Node payload) {
        for (String key : payload.keys()) {
            Object value = payload.get(key);
            if (value instanceof HashMapBackedNode) {
                if (authorization.isAuthorized(Operation.WRITE, auth, new InMemoryDataSnapshot(
                        model), path.append(key), value)) {
                    Node childNode = new HashMapBackedNode();
                    if (node.has(key)) {
                        node.put(key, childNode);
                        logBuilder.addNew(key, childNode);
                    } else {
                        node.put(key, childNode);
                        logBuilder.addChangedNode(key, childNode);
                    }
                    populate(logBuilder.getChildLogBuilder(key), path.append(key), auth, childNode,
                            (Node) value);
                }
            } else {
                if (authorization.isAuthorized(Operation.WRITE, auth, new InMemoryDataSnapshot(
                        model), path.append(key), value)) {
                    if (node.has(key)) {
                    }
                    logBuilder.addChange(key, value);
                } else {
                    logBuilder.addNew(key, value);
                }
                if (value == null) {
                    logBuilder.addRemoved(key, node.get(key));
                }
                node.put(key, value);
            }
        }
    }

    private void addChangeEvent(ChangeLog log, Path path) {
        Object payload = model.getObjectForPath(path);
        Node parent = model.getNodeForPath(log, path.getParent());
        log.addChildChangedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
                        .getParent(), payload, hasChildren(payload), childCount(payload),
                prevChildName(parent, priority(parent, path.getLastElement())),
                priority(parent, path.getLastElement())
        );

        log.addValueChangedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
                        .getParent(), payload, prevChildName(parent, priority(parent, path.getLastElement())),
                priority(parent, path.getLastElement())
        );
        if (!path.isEmtpy()) {
            addChangeEvent(log, path.getParent());
        }
    }

    @Override
    public void setPriority(ChangeLog log, Optional<Node> auth, Path path, int priority) {
        Node parent = model.getNodeForPath(log, path.getParent());
        if (authorization.isAuthorized(Operation.WRITE, auth,
                new InMemoryDataSnapshot(model), path, parent)) {
            parent.setIndexOf(path.getLastElement(), priority);
        }
    }

    @Override
    public Node getRoot() {
        return model;
    }

    public void setCore(Core core) {
        this.core = core;
    }

    public boolean exists(Path path) {
        return model.pathExists(path);
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    private final class ChildRemovedSubTreeVisitor implements NodeVisitor {
        private ChangeLog log;

        public ChildRemovedSubTreeVisitor(ChangeLog log) {
            this.log = log;
        }

        @Override
        public void visitProperty(Path path, Node node, String key, Object value) {
            log.addChildRemovedLogEntry(path, key, value);
        }

        @Override
        public void visitNode(Path path, Node node) {
            log.addChildRemovedLogEntry(path.getParent(), path.getLastElement(), null);
        }
    }
}