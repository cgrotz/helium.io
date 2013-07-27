package de.skiptag.roadrunner.messaging;

import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class RoadrunnerEndpoint implements DataListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadrunnerEndpoint.class);

    private Set<String> attached_listeners = Sets.newHashSet();
    private RoadrunnerResponseSender sender;
    private String basePath;

    public RoadrunnerEndpoint(String basePath,
	    RoadrunnerResponseSender roadrunnerResponseSender) {
	this.sender = roadrunnerResponseSender;
	this.basePath = basePath;
    }

    @Override
    public void child_added(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren, int priority) {
	if (hasListener(path)) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put(RoadrunnerEvent.TYPE, "child_added");
	    broadcast.put("name", name);
	    broadcast.put(RoadrunnerEvent.PATH, createPath(basePath, path));
	    broadcast.put("parent", parent);
	    broadcast.put(RoadrunnerEvent.PAYLOAD, node);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    broadcast.put("priority", priority);
	    sender.send(broadcast.toString());
	}
    }

    @Override
    public void child_changed(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren, int priority) {
	if (hasListener(path)) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put(RoadrunnerEvent.TYPE, "child_changed");
	    broadcast.put("name", name);
	    broadcast.put(RoadrunnerEvent.PATH, createPath(basePath, path));
	    broadcast.put("parent", parent);
	    broadcast.put(RoadrunnerEvent.PAYLOAD, node);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    broadcast.put("priority", priority);
	    sender.send(broadcast.toString());
	}
    }

    @Override
    public void value(String name, String path, String parent, Object value,
	    boolean hasChildren, long numChildren, int priority) {
	if (hasListener(path)) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put(RoadrunnerEvent.TYPE, "value");
	    broadcast.put("name", name);
	    broadcast.put(RoadrunnerEvent.PATH, createPath(basePath, path));
	    broadcast.put("parent", parent);
	    broadcast.put(RoadrunnerEvent.PAYLOAD, value);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    broadcast.put("priority", priority);
	    sender.send(broadcast.toString());
	}
    }

    @Override
    public void child_moved(JSONObject childSnapshot, boolean hasChildren,
	    long numChildren) {
	JSONObject broadcast = new JSONObject();
	broadcast.put(RoadrunnerEvent.TYPE, "child_moved");
	broadcast.put(RoadrunnerEvent.PAYLOAD, childSnapshot);
	broadcast.put("hasChildren", hasChildren);
	broadcast.put("numChildren", numChildren);
	sender.send(broadcast.toString());
    }

    @Override
    public void child_removed(String path, Object payload) {
	if (hasListener(path)) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put(RoadrunnerEvent.TYPE, "child_removed");
	    broadcast.put(RoadrunnerEvent.PATH, createPath(basePath, path));
	    broadcast.put(RoadrunnerEvent.PAYLOAD, payload);

	    sender.send(broadcast.toString());
	}
    }

    @Override
    public void distributeEvent(String path, JSONObject payload) {
	if (hasListener(path)) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put(RoadrunnerEvent.TYPE, "event");

	    broadcast.put(RoadrunnerEvent.PATH, createPath(basePath, path));
	    broadcast.put(RoadrunnerEvent.PAYLOAD, payload);
	    LOGGER.trace("Distributing Message (basePath: '" + basePath
		    + "',path: '" + path + "') : " + broadcast.toString());
	    sender.send(broadcast.toString());
	}
    }

    private String createPath(String basePath2, String path) {
	if (basePath2.endsWith("/") && path.startsWith("/")) {
	    return basePath2 + path.substring(1);
	} else {
	    return basePath2 + path;
	}
    }

    public void addListener(String path) {
	attached_listeners.add(path);
    }

    public void removeListener(String path) {
	attached_listeners.remove(path);
    }

    private boolean hasListener(String path) {
	for (String listenerPath : attached_listeners) {
	    if (path.startsWith(listenerPath)) {
		return true;
	    }
	}
	return false;
    }
}
