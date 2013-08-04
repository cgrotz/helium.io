package de.skiptag.roadrunner.messaging;

import org.json.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.disruptor.event.changelog.ChangeLogEvent;
import de.skiptag.roadrunner.disruptor.event.changelog.ChildAddedLogEvent;
import de.skiptag.roadrunner.disruptor.event.changelog.ChildChangedLogEvent;
import de.skiptag.roadrunner.disruptor.event.changelog.ChildRemovedLogEvent;
import de.skiptag.roadrunner.disruptor.event.changelog.ValueChangedLogEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryDataSnapshot;
import de.skiptag.roadrunner.queries.QueryEvaluator;

public class RoadrunnerEndpoint implements DataListener {
    private static final String QUERY_CHILD_REMOVED = "query_child_removed";

    private static final String QUERY_CHILD_CHANGED = "query_child_changed";

    private static final String QUERY_CHILD_ADDED = "query_child_added";

    private static final String CHILD_REMOVED = "child_removed";

    private static final String CHILD_MOVED = "child_moved";

    private static final String VALUE = "value";

    private static final String CHILD_CHANGED = "child_changed";

    private static final String CHILD_ADDED = "child_added";

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadrunnerEndpoint.class);

    private Multimap<String, String> attached_listeners = HashMultimap.create();
    private RoadrunnerResponseSender sender;
    private String basePath;
    private Node auth;
    private Authorization authorization;
    private Persistence persistence;
    private QueryEvaluator queryEvaluator;

    public RoadrunnerEndpoint(String basePath, Node auth,
	    RoadrunnerResponseSender roadrunnerResponseSender,
	    Persistence persistence, Authorization authorization) {
	this.sender = roadrunnerResponseSender;
	this.persistence = persistence;
	this.authorization = authorization;
	this.auth = auth;
	this.basePath = basePath;
	this.queryEvaluator = new QueryEvaluator();
    }

    @Override
    public void distribute(RoadrunnerEvent event) {
	if (event.getType() == RoadrunnerEventType.EVENT) {
	    Node jsonObject;
	    Object object = event.get(RoadrunnerEvent.PAYLOAD);
	    if (object instanceof Node) {
		jsonObject = event.getNode(RoadrunnerEvent.PAYLOAD);
		distributeEvent(event.extractNodePath(), jsonObject);
	    } else if (object instanceof String) {
		jsonObject = new Node(RoadrunnerEvent.PAYLOAD);
		distributeEvent(event.extractNodePath(), new Node(
			(String) object));
	    }
	} else {
	    for (ChangeLogEvent logE : event.getChangeLog().getLog()) {
		if (logE instanceof ChildAddedLogEvent) {
		    ChildAddedLogEvent logEvent = (ChildAddedLogEvent) logE;
		    if (hasListener(logEvent.getPath().toString(), CHILD_ADDED)) {
			fireChildAdded(logEvent.getName(), logEvent.getPath(), logEvent.getParent(), logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren(), logEvent.getPrevChildName(), logEvent.getPriority());
		    }
		    if (hasQuery(logEvent.getPath().toString())) {
			if (appliesToQuery(logEvent.getPath(), logEvent.getValue())) {
			    fireQueryChildAdded(logEvent.getName(), logEvent.getPath(), logEvent.getParent(), logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren(), logEvent.getPrevChildName(), logEvent.getPriority());
			}
		    }
		}
		if (logE instanceof ChildChangedLogEvent) {
		    ChildChangedLogEvent logEvent = (ChildChangedLogEvent) logE;
		    if (hasListener(logEvent.getPath().toString(), CHILD_CHANGED)) {
			fireChildChanged(logEvent.getName(), logEvent.getPath(), logEvent.getParent(), logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren(), logEvent.getPrevChildName(), logEvent.getPriority());
		    }
		    if (hasQuery(logEvent.getPath().toString())) {
			if (appliesToQuery(logEvent.getPath(), logEvent.getValue())) {
			    fireQueryChildChanged(logEvent.getName(), logEvent.getPath(), logEvent.getParent(), logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren(), logEvent.getPrevChildName(), logEvent.getPriority());
			}
		    }
		}
		if (logE instanceof ValueChangedLogEvent) {
		    ValueChangedLogEvent logEvent = (ValueChangedLogEvent) logE;
		    if (hasListener(logEvent.getPath().toString(), VALUE)) {
			fireValue(logEvent.getName(), logEvent.getPath(), logEvent.getParent(), logEvent.getValue(), logEvent.getPrevChildName(), logEvent.getPriority());
		    }
		}
		if (logE instanceof ChildRemovedLogEvent) {
		    ChildRemovedLogEvent logEvent = (ChildRemovedLogEvent) logE;
		    if (hasListener(logEvent.getPath().toString(), CHILD_REMOVED)) {
			fireChildRemoved(logEvent.getPath(), logEvent.getName(), logEvent.getValue());
		    }
		    if (hasQuery(logEvent.getPath().toString())) {
			if (appliesToQuery(logEvent.getPath(), logEvent.getValue())) {
			    fireQueryChildRemoved(logEvent.getPath(), logEvent.getName(), logEvent.getValue());
			}
		    }
		}
	    }
	}
    }

    public void fireChildAdded(String name, Path path, Path parent,
	    Object node, boolean hasChildren, long numChildren,
	    String prevChildName, int priority) {
	if (authorization.isAuthorized(RoadrunnerOperation.READ, auth, persistence.getRoot(), path, new InMemoryDataSnapshot(
		node))) {
	    Node broadcast = new Node();
	    broadcast.put(RoadrunnerEvent.TYPE, CHILD_ADDED);
	    broadcast.put("name", name);
	    broadcast.put(RoadrunnerEvent.PATH, createPath(path));
	    broadcast.put("parent", createPath(parent));
	    broadcast.put(RoadrunnerEvent.PAYLOAD, checkPayload(path, node));
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    broadcast.put("priority", priority);
	    sender.send(broadcast.toString());
	}
    }

    public void fireChildChanged(String name, Path path, Path parent,
	    Object node, boolean hasChildren, long numChildren,
	    String prevChildName, int priority) {
	if (node != null && node != Node.NULL) {
	    if (authorization.isAuthorized(RoadrunnerOperation.READ, auth, persistence.getRoot(), path, new InMemoryDataSnapshot(
		    node))) {
		Node broadcast = new Node();
		broadcast.put(RoadrunnerEvent.TYPE, CHILD_CHANGED);
		broadcast.put("name", name);
		broadcast.put(RoadrunnerEvent.PATH, createPath(path));
		broadcast.put("parent", createPath(parent));
		broadcast.put(RoadrunnerEvent.PAYLOAD, checkPayload(path, node));
		broadcast.put("hasChildren", hasChildren);
		broadcast.put("numChildren", numChildren);
		broadcast.put("priority", priority);
		sender.send(broadcast.toString());
	    }
	}
    }

    public void fireChildRemoved(Path path, String name, Object payload) {
	if (authorization.isAuthorized(RoadrunnerOperation.READ, auth, persistence.getRoot(), path, new InMemoryDataSnapshot(
		payload))) {
	    Node broadcast = new Node();
	    broadcast.put(RoadrunnerEvent.TYPE, CHILD_REMOVED);
	    broadcast.put(RoadrunnerEvent.NAME, name);
	    broadcast.put(RoadrunnerEvent.PATH, createPath(path));
	    broadcast.put(RoadrunnerEvent.PAYLOAD, checkPayload(path, payload));
	    sender.send(broadcast.toString());
	}
    }

    public void fireValue(String name, Path path, Path parent, Object value,
	    String prevChildName, int priority) {
	if (authorization.isAuthorized(RoadrunnerOperation.READ, auth, persistence.getRoot(), path, new InMemoryDataSnapshot(
		value))) {
	    Node broadcast = new Node();
	    broadcast.put(RoadrunnerEvent.TYPE, VALUE);
	    broadcast.put("name", name);
	    broadcast.put(RoadrunnerEvent.PATH, createPath(path));
	    broadcast.put("parent", createPath(parent));
	    broadcast.put(RoadrunnerEvent.PAYLOAD, checkPayload(path, value));
	    broadcast.put("priority", priority);
	    sender.send(broadcast.toString());
	}
    }

    public void fireChildMoved(Node childSnapshot, boolean hasChildren,
	    long numChildren) {
	Node broadcast = new Node();
	broadcast.put(RoadrunnerEvent.TYPE, CHILD_MOVED);
	broadcast.put(RoadrunnerEvent.PAYLOAD, childSnapshot);
	broadcast.put("hasChildren", hasChildren);
	broadcast.put("numChildren", numChildren);
	sender.send(broadcast.toString());
    }

    public void fireQueryChildAdded(String name, Path path, Path parent,
	    Object node, boolean hasChildren, long numChildren,
	    String prevChildName, int priority) {
	if (authorization.isAuthorized(RoadrunnerOperation.READ, auth, persistence.getRoot(), path, new InMemoryDataSnapshot(
		node))) {
	    Node broadcast = new Node();
	    broadcast.put(RoadrunnerEvent.TYPE, QUERY_CHILD_ADDED);
	    broadcast.put("name", name);
	    broadcast.put(RoadrunnerEvent.PATH, createPath(path));
	    broadcast.put("parent", createPath(parent));
	    broadcast.put(RoadrunnerEvent.PAYLOAD, checkPayload(path, node));
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    broadcast.put("priority", priority);
	    sender.send(broadcast.toString());
	}
    }

    public void fireQueryChildChanged(String name, Path path, Path parent,
	    Object node, boolean hasChildren, long numChildren,
	    String prevChildName, int priority) {
	if (node != null && node != Node.NULL) {
	    if (authorization.isAuthorized(RoadrunnerOperation.READ, auth, persistence.getRoot(), path, new InMemoryDataSnapshot(
		    node))) {
		Node broadcast = new Node();
		broadcast.put(RoadrunnerEvent.TYPE, QUERY_CHILD_CHANGED);
		broadcast.put("name", name);
		broadcast.put(RoadrunnerEvent.PATH, createPath(path));
		broadcast.put("parent", createPath(parent));
		broadcast.put(RoadrunnerEvent.PAYLOAD, checkPayload(path, node));
		broadcast.put("hasChildren", hasChildren);
		broadcast.put("numChildren", numChildren);
		broadcast.put("priority", priority);
		sender.send(broadcast.toString());
	    }
	}
    }

    public void fireQueryChildRemoved(Path path, String name, Object payload) {
	if (authorization.isAuthorized(RoadrunnerOperation.READ, auth, persistence.getRoot(), path, new InMemoryDataSnapshot(
		payload))) {
	    Node broadcast = new Node();
	    broadcast.put(RoadrunnerEvent.TYPE, QUERY_CHILD_REMOVED);
	    broadcast.put(RoadrunnerEvent.NAME, name);
	    broadcast.put(RoadrunnerEvent.PATH, createPath(path));
	    broadcast.put(RoadrunnerEvent.PAYLOAD, checkPayload(path, payload));
	    sender.send(broadcast.toString());
	}
    }

    public void distributeEvent(String path, Node payload) {
	if (hasListener(path, "event")) {
	    Node broadcast = new Node();
	    broadcast.put(RoadrunnerEvent.TYPE, "event");

	    broadcast.put(RoadrunnerEvent.PATH, createPath(path));
	    broadcast.put(RoadrunnerEvent.PAYLOAD, payload);
	    LOGGER.trace("Distributing Message (basePath: '" + basePath
		    + "',path: '" + path + "') : " + broadcast.toString());
	    sender.send(broadcast.toString());
	}
    }

    private Object checkPayload(Path path, Object value) {
	if (value instanceof Node) {
	    Node org = (Node) value;
	    Node node = new Node();
	    for (String key : org.keys()) {
		if (authorization.isAuthorized(RoadrunnerOperation.READ, auth, persistence.getRoot(), path.append(key), new InMemoryDataSnapshot(
			org.get(key)))) {
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

    public void addListener(String path, String type) {
	attached_listeners.put(path, type);
    }

    public void removeListener(String path, String type) {
	attached_listeners.remove(path, type);
    }

    private boolean hasListener(String path, String type) {
	return attached_listeners.containsKey(path)
		&& attached_listeners.get(path).contains(type);
    }

    public void addQuery(String path, String query) {
	queryEvaluator.addQuery(path, query);
    }

    public void removeQuery(String path, String query) {
	queryEvaluator.removeQuery(path, query);
    }

    public boolean hasQuery(String path) {
	return queryEvaluator.hasQuery(path);
    }

    private boolean appliesToQuery(Path path, Object value) {
	return queryEvaluator.appliesToQuery(path, value);
    }
}
