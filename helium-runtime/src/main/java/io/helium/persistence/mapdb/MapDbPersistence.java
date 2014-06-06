package io.helium.persistence.mapdb;

import io.helium.authorization.Authorizator;
import io.helium.authorization.Operation;
import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;
import io.helium.persistence.ChildRemovedSubTreeVisitor;
import io.helium.server.distributor.Distributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.Optional;

/**
 * Created by Christoph Grotz on 02.06.14.
 */
public class MapDbPersistence {
    private static final Logger logger = LoggerFactory.getLogger(MapDbPersistence.class);
    private final Vertx vertx;

    public MapDbPersistence(Vertx vertx) {
        this.vertx = vertx;
    }

    public Object get(Path path) {
        if (path == null || path.isEmtpy()) {
            return MapDbBackedNode.of(path);
        } else {
            return MapDbBackedNode.of(path.parent()).getObjectForPath(path);
        }
    }

    public MapDbBackedNode getNode(ChangeLog log, Path path) {
        return MapDbBackedNode.of(path);
    }


    public void remove(ChangeLog log, Optional<JsonObject> auth, Path path) {
        MapDbBackedNode parent = MapDbBackedNode.of(path.parent());
        Object value = parent.get(path.lastElement());

        vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.WRITE, auth, path, value), (Message<Boolean> event) -> {
            if (event.body()) {
                if (value instanceof MapDbBackedNode) {
                    ((MapDbBackedNode) value).accept(path, new ChildRemovedSubTreeVisitor(log));
                }
                parent.remove(path.lastElement());
                log.addChildRemovedLogEntry(path.parent(), path.lastElement(), value);
                vertx.eventBus().send(Distributor.DISTRIBUTE_CHANGE_LOG, log);
            }
        });
    }

    public void updateValue(ChangeLog log, long sequence, Optional<JsonObject> auth, Path path, int priority, Object payload) {
        MapDbBackedNode node;
        boolean created = false;
        if (!exists(path)) {
            created = true;
        }
        MapDbBackedNode parent;
        if (exists(path.parent())) {
            parent = MapDbBackedNode.of(path.parent());
        } else {
            parent = MapDbBackedNode.of(path.parent().parent());
        }

        if (payload instanceof MapDbBackedNode) {
            if (parent.has(path.lastElement())) {
                node = parent.getNode(path.lastElement());
                parent.setIndexOf(path.lastElement(), priority);
            } else {
                node = MapDbBackedNode.of(path.append(path.lastElement()));
                parent.putWithIndex(path.lastElement(), node, priority);
            }
            node.populate(new ChangeLogBuilder(log, sequence, path, path.parent(), node), (MapDbBackedNode) payload);
            if (created) {
                log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                        MapDbBackedNode.priority(parent, path.lastElement())
                );
            } else {
                log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                        MapDbBackedNode.priority(parent, path.lastElement())
                );
            }
        } else {
            parent.putWithIndex(path.lastElement(), payload, priority);

            if (created) {
                log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                        MapDbBackedNode.priority(parent, path.lastElement())
                );
            } else {
                log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                                .parent(), payload, false, 0,
                        MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                        MapDbBackedNode.priority(parent, path.lastElement())
                );
                log.addValueChangedLogEntry(path.lastElement(), path, path.parent(), payload,
                        MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                        MapDbBackedNode.priority(parent, path.lastElement()));
            }
            log.addChildChangedLogEntry(path.parent().lastElement(), path.parent().parent(),
                    path.parent().parent().parent(), parent, false, 0,
                    MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                    MapDbBackedNode.priority(parent, path.lastElement()));

        }
    }


    public void applyNewValue(ChangeLog log, long sequence, Optional<JsonObject> auth, Path path, int priority, Object payload) {
        vertx.eventBus().send(Authorizator.IS_AUTHORIZED,
                Authorizator.check(Operation.WRITE, auth, path, payload),
                (Message<Object> event) -> {
                    boolean created = false;
                    if (!exists(path)) {
                        created = true;
                    }

                    MapDbBackedNode parent;
                    if (exists(path.parent())) {
                        parent = MapDbBackedNode.of(path.parent());
                    } else {
                        parent = MapDbBackedNode.of(path.parent().parent());
                    }

                    if (payload instanceof MapDbBackedNode) {
                        MapDbBackedNode node = (MapDbBackedNode) payload;
                        populate(new ChangeLogBuilder(log, sequence, path, path.parent(), node), path, auth, node,
                                (MapDbBackedNode) payload);
                        parent.putWithIndex(path.lastElement(), node, priority);
                    } else {
                        parent.putWithIndex(path.lastElement(), payload, priority);
                    }

                    if (created) {
                        log.addChildAddedLogEntry(path.lastElement(), path.parent(), path.parent()
                                        .parent(), payload, false, 0,
                                MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                                MapDbBackedNode.priority(parent, path.lastElement())
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
        );
    }

    private Object getObjectForPath(Path path) {
        return MapDbBackedNode.of(path.parent()).get(path.lastElement());
    }

    public void populate(ChangeLogBuilder logBuilder, Path path, Optional<JsonObject> auth, MapDbBackedNode node, MapDbBackedNode payload) {
        for (String key : payload.keys()) {
            Object value = payload.get(key);
            if (value instanceof MapDbBackedNode) {
                vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.WRITE, auth, path.append(key), value), new Handler<Message<Boolean>>() {
                    @Override
                    public void handle(Message<Boolean> event) {
                        if (event.body()) {
                            MapDbBackedNode childNode = MapDbBackedNode.of(path.append(key));
                            if (node.has(key)) {
                                node.put(key, childNode);
                                logBuilder.addNew(key, childNode);
                            } else {
                                node.put(key, childNode);
                                logBuilder.addChangedNode(key, childNode);
                            }
                            populate(logBuilder.getChildLogBuilder(key), path.append(key), auth, childNode,
                                    (MapDbBackedNode) value);
                        }
                    }
                });
            } else {
                vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.WRITE, auth, path.append(key), value), (Message<Boolean> event) -> {
                    if (event.body()) {
                        if (node.has(key)) {
                            logBuilder.addChange(key, value);
                        } else {
                            logBuilder.addNew(key, value);
                        }
                        if (value == null) {
                            logBuilder.addRemoved(key, node.get(key));
                        }
                        node.put(key, value);
                    }
                });
            }
        }
    }

    private void addChangeEvent(ChangeLog log, Path path) {
        Object payload = getObjectForPath(path);
        MapDbBackedNode parent;
        if (exists(path.parent())) {
            parent = MapDbBackedNode.of(path.parent());
        } else {
            parent = MapDbBackedNode.of(path.parent().parent());
        }

        log.addChildChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                        .parent(), payload, MapDbBackedNode.hasChildren(payload), MapDbBackedNode.childCount(payload),
                MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                MapDbBackedNode.priority(parent, path.lastElement())
        );

        log.addValueChangedLogEntry(path.lastElement(), path.parent(), path.parent()
                        .parent(), payload, MapDbBackedNode.prevChildName(parent, MapDbBackedNode.priority(parent, path.lastElement())),
                MapDbBackedNode.priority(parent, path.lastElement())
        );
        if (!path.isEmtpy()) {
            addChangeEvent(log, path.parent());
        }
    }


    public void setPriority(ChangeLog log, Optional<JsonObject> auth, Path path, int priority) {
        MapDbBackedNode parent;
        if (exists(path.parent())) {
            parent = MapDbBackedNode.of(path.parent());
        } else {
            parent = MapDbBackedNode.of(path.parent().parent());
        }

        vertx.eventBus().send(Authorizator.IS_AUTHORIZED,
                Authorizator.check(Operation.WRITE, auth, path, parent),
                (Message<Boolean> event) -> {
                    if (event.body()) {
                        parent.setIndexOf(path.lastElement(), priority);
                    }
                }
        );
    }

    public boolean exists(Path path) {
        return MapDbBackedNode.exists(path);
    }
}
