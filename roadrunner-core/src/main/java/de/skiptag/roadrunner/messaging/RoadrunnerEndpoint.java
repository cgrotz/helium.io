package de.skiptag.roadrunner.messaging;

import org.json.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.disruptor.event.changelog.ChangeLogEvent;
import de.skiptag.roadrunner.disruptor.event.changelog.ChildAddedLogEvent;
import de.skiptag.roadrunner.disruptor.event.changelog.ChildChangedLogEvent;
import de.skiptag.roadrunner.disruptor.event.changelog.ChildRemovedLogEvent;
import de.skiptag.roadrunner.disruptor.event.changelog.ValueChangedLogEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class RoadrunnerEndpoint implements DataListener {

    private static final String CHILD_REMOVED = "child_removed";

    private static final String CHILD_MOVED = "child_moved";

    private static final String VALUE = "value";

    private static final String CHILD_CHANGED = "child_changed";

    private static final String CHILD_ADDED = "child_added";

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadrunnerEndpoint.class);

    private Multimap<String, String> attached_listeners = HashMultimap.create();
    private RoadrunnerResponseSender sender;
    private String basePath;

    private Persistence persistence;

    public RoadrunnerEndpoint(String basePath,
	    RoadrunnerResponseSender roadrunnerResponseSender,
	    Persistence persistence) {
	this.sender = roadrunnerResponseSender;
	this.basePath = basePath;
	this.persistence = persistence;
    }

    @Override
    public void distribute(RoadrunnerEvent event) {
	for (ChangeLogEvent logEvent : Sets.filter(event.getChangeLog()
		.getLog(), Predicates.instanceOf(ChildAddedLogEvent.class))) {
	    ChildAddedLogEvent childAddedLogEvent = (ChildAddedLogEvent) logEvent;
	    if (hasListener(childAddedLogEvent.getParent().toString(), CHILD_ADDED)) {
		fireChildAdded(childAddedLogEvent.getName(), createPath(childAddedLogEvent.getParent()), createPath(childAddedLogEvent.getParent()), childAddedLogEvent.getValue(), childAddedLogEvent.getHasChildren(), childAddedLogEvent.getNumChildren(), childAddedLogEvent.getPrevChildName(), childAddedLogEvent.getPriority());
	    }
	}
	for (ChangeLogEvent logEvent : Sets.filter(event.getChangeLog()
		.getLog(), Predicates.instanceOf(ChildChangedLogEvent.class))) {
	    ChildChangedLogEvent childChangedLogEvent = (ChildChangedLogEvent) logEvent;
	    if (hasListener(childChangedLogEvent.getPath().toString(), CHILD_CHANGED)) {
		fireChildChanged(childChangedLogEvent.getName(), createPath(childChangedLogEvent.getPath()), createPath(childChangedLogEvent.getParent()), childChangedLogEvent.getValue(), childChangedLogEvent.getHasChildren(), childChangedLogEvent.getNumChildren(), childChangedLogEvent.getPrevChildName(), childChangedLogEvent.getPriority());
	    }
	}
	for (ChangeLogEvent logEvent : Sets.filter(event.getChangeLog()
		.getLog(), Predicates.instanceOf(ValueChangedLogEvent.class))) {
	    ValueChangedLogEvent valueChangedLogEvent = (ValueChangedLogEvent) logEvent;
	    if (hasListener(valueChangedLogEvent.getPath().toString(), VALUE)) {
		fireValue(valueChangedLogEvent.getName(), createPath(valueChangedLogEvent.getPath()), createPath(valueChangedLogEvent.getParent()), valueChangedLogEvent.getValue(), valueChangedLogEvent.getPrevChildName(), valueChangedLogEvent.getPriority());
	    }
	}
	for (ChangeLogEvent logEvent : Sets.filter(event.getChangeLog()
		.getLog(), Predicates.instanceOf(ChildRemovedLogEvent.class))) {
	    ChildRemovedLogEvent childRemovedLogEvent = (ChildRemovedLogEvent) logEvent;
	    if (hasListener(childRemovedLogEvent.getPath()
		    .getParent()
		    .toString(), CHILD_REMOVED)) {
		fireChildRemoved(createPath(childRemovedLogEvent.getPath()), childRemovedLogEvent.getName(), childRemovedLogEvent.getValue());
	    }
	}
	String path = event.extractNodePath();
	RoadrunnerEventType type = event.getType();
	if (type == RoadrunnerEventType.EVENT) {
	    Node jsonObject;
	    Object object = event.get(RoadrunnerEvent.PAYLOAD);
	    if (object instanceof Node) {
		jsonObject = event.getNode(RoadrunnerEvent.PAYLOAD);
		distributeEvent(path, jsonObject);
	    } else if (object instanceof String) {
		jsonObject = new Node(RoadrunnerEvent.PAYLOAD);
		distributeEvent(path, new Node((String) object));
	    }
	}
    }

    public void fireChildAdded(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren,
	    String prevChildName, int priority) {
	Node broadcast = new Node();
	broadcast.put(RoadrunnerEvent.TYPE, CHILD_ADDED);
	broadcast.put("name", name);
	broadcast.put(RoadrunnerEvent.PATH, path);
	broadcast.put("parent", parent);
	broadcast.put(RoadrunnerEvent.PAYLOAD, node);
	broadcast.put("hasChildren", hasChildren);
	broadcast.put("numChildren", numChildren);
	broadcast.put("priority", priority);
	sender.send(broadcast.toString());
    }

    public void fireChildChanged(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren,
	    String prevChildName, int priority) {
	if (node != null && node != Node.NULL) {
	    Node broadcast = new Node();
	    broadcast.put(RoadrunnerEvent.TYPE, CHILD_CHANGED);
	    broadcast.put("name", name);
	    broadcast.put(RoadrunnerEvent.PATH, path);
	    broadcast.put("parent", parent);
	    broadcast.put(RoadrunnerEvent.PAYLOAD, node);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    broadcast.put("priority", priority);
	    sender.send(broadcast.toString());
	}
    }

    public void fireValue(String name, String path, String parent,
	    Object value, String prevChildName, int priority) {
	Node broadcast = new Node();
	broadcast.put(RoadrunnerEvent.TYPE, VALUE);
	broadcast.put("name", name);
	broadcast.put(RoadrunnerEvent.PATH, path);
	broadcast.put("parent", parent);
	broadcast.put(RoadrunnerEvent.PAYLOAD, value);
	broadcast.put("priority", priority);
	sender.send(broadcast.toString());
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

    public void fireChildRemoved(String path, String name, Object payload) {
	Node broadcast = new Node();
	broadcast.put(RoadrunnerEvent.TYPE, CHILD_REMOVED);
	broadcast.put(RoadrunnerEvent.NAME, name);
	broadcast.put(RoadrunnerEvent.PATH, path);
	broadcast.put(RoadrunnerEvent.PAYLOAD, payload);

	sender.send(broadcast.toString());
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
}
