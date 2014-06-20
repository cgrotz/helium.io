package io.helium.persistence.actions;

import io.helium.authorization.Authorizator;
import io.helium.authorization.Operation;
import io.helium.common.Path;
import io.helium.event.changelog.*;
import io.helium.persistence.mapdb.Node;
import io.helium.persistence.visitor.ChildDeletedSubTreeVisitor;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
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
        } else if ( path.isEmtpy()) {
            return Node.of(path);
        } else {
            return Node.of(path.parent()).getObjectForPath(path);
        }
    }

    protected void applyNewValue( Optional<JsonObject> auth,
                                 Path path,
                                 Object payload,
                                 Handler<ChangeLog> handler ) {
        vertx.eventBus().send(Authorizator.CHECK,
                Authorizator.check(Operation.WRITE, auth, path, payload),
                (Message<Boolean> event) -> {
                    if (event.body()) {
                        ChangeLog changeLog = new ChangeLog(new JsonArray());

                        Node parent;
                        if (exists(path.parent())) {
                            parent = Node.of(path.parent());
                        } else {
                            parent = Node.of(path.parent().parent());
                        }

                        if (payload instanceof JsonObject) {
                            Node node = Node.of(path);
                            populate(new ChangeLogBuilder(changeLog, path, path.parent(), node),
                                    path, auth, node,
                                    (JsonObject) payload);
                        }

                        if (!exists(path)) {
                            changeLog.addChildAddedLogEntry(path.lastElement(),
                                    path.parent(), path.parent().parent(), payload, false, 0);
                        } else if(!payload.equals(parent.get(path.lastElement()))) {
                            addChangeEvent(changeLog, path);
                        }

                        Path currentPath = path;
                        while (!currentPath.isSimple()) {
                            if(!payload.equals(parent.get(path.lastElement()))) {
                                changeLog.addValueChangedLogEntry(currentPath.lastElement(), currentPath,
                                        currentPath.parent(), getObjectForPath(currentPath));
                                currentPath = currentPath.parent();
                            }
                        }
                        handler.handle(changeLog);
                    }
                }
        );
    }


    protected void populate(ChangeLogBuilder logBuilder, Path path, Optional<JsonObject> auth, Node node, JsonObject payload) {
        for (String key : payload.getFieldNames()) {
            Object value = payload.getField(key);
            if (value instanceof JsonObject) {
                vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, path.append(key), value), (Message<Boolean> event) -> {
                    if (event.body()) {
                        Node childNode = Node.of(path.append(key));
                        if (node.has(key)) {
                            logBuilder.addNew(key, childNode);
                        } else {
                            logBuilder.addChangedNode(key, childNode);
                        }
                        populate(logBuilder.getChildLogBuilder(key), path.append(key), auth, childNode,
                                (JsonObject) value);
                    }
                });
            } else {
                vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, path.append(key), value), (Message<Boolean> event) -> {
                    if (event.body()) {
                        vertx.eventBus().send(Authorizator.VALIDATE,
                            Authorizator.validate(auth, path.append(key), value),
                                (Message<Object> event1) -> {
                                    if (node.has(key)) {
                                        logBuilder.addChange(key, event1.body());
                                    } else {
                                        logBuilder.addNew(key, event1.body());
                                    }
                                    if (event1.body() == null) {
                                        logBuilder.addDeleted(key, node.get(key));
                                    }
                                }
                        );
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


    protected void delete(Optional<JsonObject> auth, Path path, Handler<ChangeLog> handler) {
        Node parent = Node.of(path.parent());
        Object value = parent.get(path.lastElement());

        vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, path, value), (Message<Boolean> event) -> {
            if (event.body()) {
                ChangeLog changeLog = new ChangeLog(new JsonArray());
                if (value instanceof Node) {
                    ((Node) value).accept(path, new ChildDeletedSubTreeVisitor(changeLog));
                }
                changeLog.addChildDeletedLogEntry(path.parent(), path.lastElement(), value);
                handler.handle(changeLog);
            }
        });
    }
}