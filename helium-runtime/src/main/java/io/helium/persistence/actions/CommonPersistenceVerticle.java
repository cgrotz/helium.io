package io.helium.persistence.actions;

import io.helium.authorization.Authorizator;
import io.helium.authorization.Operation;
import io.helium.common.EndpointConstants;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;
import io.helium.persistence.visitor.ChildDeletedSubTreeVisitor;
import io.helium.persistence.mapdb.Node;
import io.helium.persistence.mapdb.NodeFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.Optional;

/**
 * Created by Christoph Grotz on 15.06.14.
 */
public abstract class CommonPersistenceVerticle extends Verticle {

    protected Object get(Path path) {
        if (path.root()) {
            return Node.root();
        } else if (path == null || path.isEmtpy()) {
            return Node.of(path);
        } else {
            return Node.of(path.parent()).getObjectForPath(path);
        }
    }

    protected void applyNewValue(HeliumEvent heliumEvent, Optional<JsonObject> auth, Path path, Object payload) {
        vertx.eventBus().send(Authorizator.CHECK,
                Authorizator.check(Operation.WRITE, auth, path, payload),
                new Handler<Message<Boolean>>() {
                    @Override
                    public void handle(Message<Boolean> event) {
                        if (event.body()) {
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

                            if (payload instanceof Node) {
                                Node node = (Node) payload;
                                populate(new ChangeLogBuilder(heliumEvent.getChangeLog(), path, path.parent(), node), path, auth, node,
                                        (Node) payload);
                                parent.putWithIndex(path.lastElement(), node);
                            } else {
                                parent.putWithIndex(path.lastElement(), payload);
                            }

                            if (created) {
                                heliumEvent.getChangeLog().addChildAddedLogEntry(path.lastElement(),
                                        path.parent(), path.parent().parent(), payload, false, 0);
                            } else {
                                addChangeEvent(heliumEvent.getChangeLog(), path);
                            }
                            {
                                Path currentPath = path;
                                while (!currentPath.isSimple()) {
                                    heliumEvent.getChangeLog().addValueChangedLogEntry(currentPath.lastElement(), currentPath,
                                            currentPath.parent(), getObjectForPath(currentPath));
                                    currentPath = currentPath.parent();
                                }
                            }

                            vertx.eventBus().publish(EndpointConstants.DISTRIBUTE_HELIUM_EVENT, heliumEvent);
                            //MapDbBackedNode.getDb().commit();
                        }
                    }
                }
        );
    }


    protected void populate(ChangeLogBuilder logBuilder, Path path, Optional<JsonObject> auth, Node node, Node payload) {
        for (String key : payload.keys()) {
            Object value = payload.get(key);
            if (value instanceof Node) {
                vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, path.append(key), value), new Handler<Message<Boolean>>() {
                    @Override
                    public void handle(Message<Boolean> event) {
                        if (event.body()) {
                            Node childNode = Node.of(path.append(key));
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
                    }
                });
            } else {
                vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, path.append(key), value), (Message<Boolean> event) -> {
                    if (event.body()) {
                        if (node.has(key)) {
                            logBuilder.addChange(key, value);
                        } else {
                            logBuilder.addNew(key, value);
                        }
                        if (value == null) {
                            logBuilder.addDeleted(key, node.get(key));
                        }
                        node.put(key, value);
                    }
                });
            }
        }
    }

    private Object getObjectForPath(Path path) {
        Node node = Node.of(path.parent());
        if (node.has(path.lastElement())) {
            return node.get(path.lastElement());
        } else {
            return null;
        }
    }

    private void addChangeEvent(ChangeLog log, Path path) {
        Object payload = getObjectForPath(path);
        Node parent;
        if (exists(path.parent())) {
            parent = Node.of(path.parent());
        } else {
            parent = Node.of(path.parent().parent());
        }

        log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                .parent(), payload, Node.hasChildren(payload), Node.childCount(payload));

        log.addValueChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                .parent(), payload);
        if (!path.isEmtpy()) {
            addChangeEvent(log, path.parent());
        }
    }

    protected boolean exists(Path path) {
        return Node.exists(path);
    }


    protected void delete(HeliumEvent heliumEvent, Optional<JsonObject> auth, Path path) {
        Node parent = Node.of(path.parent());
        Object value = parent.get(path.lastElement());

        vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, path, value), new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> event) {
                if (event.body()) {
                    if (value instanceof Node) {
                        ((Node) value).accept(path, new ChildDeletedSubTreeVisitor(heliumEvent.getChangeLog()));
                    }
                    parent.delete(path.lastElement());
                    heliumEvent.getChangeLog().addChildDeletedLogEntry(path.parent(), path.lastElement(), value);
                    vertx.eventBus().publish(EndpointConstants.DISTRIBUTE_CHANGE_LOG, heliumEvent.getChangeLog());
                    vertx.eventBus().publish(EndpointConstants.DISTRIBUTE_HELIUM_EVENT, heliumEvent);
                    NodeFactory.get().getDb().commit();
                }
            }
        });
    }
}
