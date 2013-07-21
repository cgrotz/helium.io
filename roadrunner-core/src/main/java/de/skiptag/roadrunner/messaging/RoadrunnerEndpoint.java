package de.skiptag.roadrunner.messaging;

import java.util.Set;

import org.json.JSONObject;

import com.google.common.collect.Sets;

public class RoadrunnerEndpoint implements DataListener {

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
	    Object node, boolean hasChildren, long numChildren) {
	if (hasListener(path)) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_added");
	    broadcast.put("name", name);
	    broadcast.put("path", basePath + path);
	    broadcast.put("parent", parent);
	    broadcast.put("payload", node);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    sender.send(broadcast.toString());
	}
    }

    @Override
    public void child_changed(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren) {
	if (hasListener(path)) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_changed");
	    broadcast.put("name", name);
	    broadcast.put("path", basePath + path);
	    broadcast.put("parent", parent);
	    broadcast.put("payload", node);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    sender.send(broadcast.toString());
	}
    }

    @Override
    public void child_moved(JSONObject childSnapshot, boolean hasChildren,
	    long numChildren) {
	JSONObject broadcast = new JSONObject();
	broadcast.put("type", "child_moved");
	broadcast.put("payload", childSnapshot);
	broadcast.put("hasChildren", hasChildren);
	broadcast.put("numChildren", numChildren);
	sender.send(broadcast.toString());
    }

    @Override
    public void child_removed(String path, Object payload) {
	if (hasListener(path)) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_removed");
	    broadcast.put("path", basePath + path);
	    broadcast.put("payload", payload);

	    sender.send(broadcast.toString());
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
