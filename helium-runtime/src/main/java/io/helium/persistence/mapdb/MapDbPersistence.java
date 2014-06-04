package io.helium.persistence.mapdb;

import io.helium.common.Path;
import io.helium.core.Core;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.persistence.ChildRemovedSubTreeVisitor;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.queries.QueryEvaluator;
import io.helium.server.Endpoint;
import io.helium.server.protocols.websocket.WebsocketEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by Christoph Grotz on 02.06.14.
 */
public class MapDbPersistence implements Persistence {
    private static final Logger logger = LoggerFactory.getLogger(MapDbPersistence.class);
    private Core core;
    private Authorization authorization;

    @Override
    public Object get(Path path) {
        if (path == null || path.isEmtpy()) {
            return MapDbBackedNode.of(path);
        } else {
            return MapDbBackedNode.of(path.parent()).getObjectForPath(path);
        }
    }

    @Override
    public Node getNode(ChangeLog log, Path path) {
        return MapDbBackedNode.of(path);
    }

    @Override
    public void remove(ChangeLog log, Optional<Node> auth, Path path) {
        Node parent = MapDbBackedNode.of(path.parent());
        Object value = parent.get(path.lastElement());

        if (authorization.isAuthorized(Operation.WRITE, auth, path, value)) {
            if(value instanceof Node) {
                ((Node)value).accept(path, new ChildRemovedSubTreeVisitor(log));
            }
            parent.remove(path.lastElement());
            log.addChildRemovedLogEntry(path.parent(), path.lastElement(), value);
            core.distributeChangeLog(log);
        }
    }

    @Override
    public void syncPath(ChangeLog log, Path path, Endpoint handler) {
        Node node;
        if(MapDbBackedNode.exists(path)) {
            node = MapDbBackedNode.of(path);
        }
        else {
            node = MapDbBackedNode.of(path.parent());
        }

        for (String childNodeKey : node.keys()) {
            Object object = node.get(childNodeKey);
            boolean hasChildren = (object instanceof Node) ? ((Node) object).hasChildren() : false;
            int indexOf = node.indexOf(childNodeKey);
            int numChildren = (object instanceof Node) ? ((Node) object).length() : 0;
            if (object != null && object != HashMapBackedNode.NULL) {
                handler.fireChildAdded(childNodeKey, path, path.parent(), object, hasChildren,
                        numChildren, null, indexOf);
            }
        }
    }

    @Override
    public void syncPathWithQuery(ChangeLog log, Path path, WebsocketEndpoint handler,
                                  QueryEvaluator queryEvaluator, String query) {
        Node node;
        if(MapDbBackedNode.exists(path)) {
            node = MapDbBackedNode.of(path);
        }
        else {
            node = MapDbBackedNode.of(path.parent());
        }

        for (String childNodeKey : node.keys()) {
            Object object = node.get(childNodeKey);
            if (queryEvaluator.evaluateQueryOnValue(object, query)) {
                if (object != null && object != HashMapBackedNode.NULL) {
                    handler.fireQueryChildAdded(path, node, object);
                }
            }
        }
    }

    @Override
    public void syncPropertyValue(ChangeLog log, Path path, Endpoint handler) {
        Node node = MapDbBackedNode.of(path.parent());
        String childNodeKey = path.lastElement();
        if (node.has(path.lastElement())) {
            Object object = node.get(path.lastElement());
            handler.fireValue(childNodeKey, path, path.parent(), object, "",
                    node.indexOf(childNodeKey));
        } else {
            handler.fireValue(childNodeKey, path, path.parent(), "", "", node.indexOf(childNodeKey));
        }
    }

    @Override
    public void updateValue(ChangeLog log, long sequence, Optional<Node> auth, Path path, int priority, Object payload) {
        Node node;
        boolean created = false;
        if (!MapDbBackedNode.exists(path)) {
            created = true;
        }
        Node parent;
        if(MapDbBackedNode.exists(path.parent())) {
            parent = MapDbBackedNode.of(path.parent());
        }
        else {
            parent = MapDbBackedNode.of(path.parent().parent());
        }

        if (payload instanceof Node) {
            if (parent.has(path.lastElement())) {
                node = parent.getNode(path.lastElement());
                parent.setIndexOf(path.lastElement(), priority);
            } else {
                node = new HashMapBackedNode();
                parent.putWithIndex(path.lastElement(), node, priority);
            }
            node.populate(new ChangeLogBuilder(log, sequence, path, path.parent(), node), (Node) payload);
            if (created) {
                log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                        Node.priority(parent, path.lastElement())
                );
            } else {
                log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                        Node.priority(parent, path.lastElement())
                );
            }
        } else {
            parent.putWithIndex(path.lastElement(), payload, priority);

            if (created) {
                log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                        Node.priority(parent, path.lastElement())
                );
            } else {
                log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                        Node.priority(parent, path.lastElement())
                );
                log.addValueChangedLogEntry(path.lastElement(), path, path.parent(), payload,
                        Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                        Node.priority(parent, path.lastElement()));
            }
            log.addChildChangedLogEntry(path.parent().lastElement(), path.parent().parent(),
                    path.parent().parent().parent(), parent, false, 0,
                    Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                    Node.priority(parent, path.lastElement()));

        }
    }

    @Override
    public void applyNewValue(ChangeLog log, long sequence, Optional<Node> auth, Path path, int priority, Object payload) {
        boolean created = false;
        if (!MapDbBackedNode.exists(path)) {
            created = true;
        }
        if (authorization.isAuthorized(Operation.WRITE, auth,
                path, payload)) {
            Node parent;
            if(MapDbBackedNode.exists(path.parent())) {
                parent = MapDbBackedNode.of(path.parent());
            }
            else {
                parent = MapDbBackedNode.of(path.parent().parent());
            }

            if (payload instanceof Node) {
                Node node = new HashMapBackedNode();
                populate(new ChangeLogBuilder(log, sequence,path, path.parent(), node), path, auth, node,
                        (Node) payload);
                parent.putWithIndex(path.lastElement(), node, priority);
            } else {
                parent.putWithIndex(path.lastElement(), payload, priority);
            }

            if (created) {
                log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                        Node.priority(parent, path.lastElement())
                );
            } else {
                addChangeEvent(log, path);
            }
            {
                Path currentPath = path;
                while (!currentPath.isSimple()) {
                    log.addValueChangedLogEntry(currentPath.lastElement(), currentPath,
                            currentPath.parent(), getObjectForPath(currentPath), null, -1);
                    currentPath = currentPath.parent();
                }
            }
        }
    }

    private Object getObjectForPath(Path path) {
        return MapDbBackedNode.of(path.parent()).get(path.lastElement());
    }

    public void populate(ChangeLogBuilder logBuilder, Path path, Optional<Node> auth, Node node, Node payload) {
        for (String key : payload.keys()) {
            Object value = payload.get(key);
            if (value instanceof Node) {
                if (authorization.isAuthorized(Operation.WRITE, auth, path.append(key), value)) {
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
                if (authorization.isAuthorized(Operation.WRITE, auth, path.append(key), value)) {
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
        Object payload = getObjectForPath(path);
        Node parent;
        if(MapDbBackedNode.exists(path.parent())) {
            parent = MapDbBackedNode.of(path.parent());
        }
        else {
            parent = MapDbBackedNode.of(path.parent().parent());
        }

        log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                        .parent(), payload, Node.hasChildren(payload), Node.childCount(payload),
                Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                Node.priority(parent, path.lastElement())
        );

        log.addValueChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                        .parent(), payload, Node.prevChildName(parent, Node.priority(parent, path.lastElement())),
                Node.priority(parent, path.lastElement())
        );
        if (!path.isEmtpy()) {
            addChangeEvent(log, path.parent());
        }
    }

    @Override
    public void setPriority(ChangeLog log, Optional<Node> auth, Path path, int priority) {
        Node parent;
        if(MapDbBackedNode.exists(path.parent())) {
            parent = MapDbBackedNode.of(path.parent());
        }
        else {
            parent = MapDbBackedNode.of(path.parent().parent());
        }

        if (authorization.isAuthorized(Operation.WRITE, auth,
                path, parent)) {
            parent.setIndexOf(path.lastElement(), priority);
        }
    }

    public void setCore(Core core) {
        this.core = core;
    }

    public boolean exists(Path path) {
        return MapDbBackedNode.exists(path);
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }
}
