package de.skiptag.roadrunner.messaging;

import java.util.Set;

import org.json.JSONObject;

import com.google.common.collect.Sets;

public class RoadrunnerEventHandler implements DataListener {

    private RoadrunnerResponseSender roadrunnerSender;

    private Set<String> attached_listeners = Sets.newHashSet();

    public RoadrunnerEventHandler(RoadrunnerResponseSender roadrunnerSender) {
	this.roadrunnerSender = roadrunnerSender;
    }

    @Override
    public void child_added(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren) {

	JSONObject broadcast = new JSONObject();
	broadcast.put("type", "child_added");
	broadcast.put("name", name);
	broadcast.put("path", path);
	broadcast.put("parent", parent);
	broadcast.put("payload", node);
	broadcast.put("hasChildren", hasChildren);
	broadcast.put("numChildren", numChildren);
	if (hasListener(path)) {
	    roadrunnerSender.send(broadcast.toString());
	}
    }

    @Override
    public void child_changed(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren) {
	JSONObject broadcast = new JSONObject();
	broadcast.put("type", "child_changed");
	broadcast.put("name", name);
	broadcast.put("path", path);
	broadcast.put("parent", parent);
	broadcast.put("payload", node);
	broadcast.put("hasChildren", hasChildren);
	broadcast.put("numChildren", numChildren);
	roadrunnerSender.send(broadcast.toString());
    }

    @Override
    public void child_moved(JSONObject childSnapshot, boolean hasChildren,
	    long numChildren) {
	JSONObject broadcast = new JSONObject();
	broadcast.put("type", "child_moved");
	broadcast.put("payload", childSnapshot);
	broadcast.put("hasChildren", hasChildren);
	broadcast.put("numChildren", numChildren);
	roadrunnerSender.send(broadcast.toString());
    }

    @Override
    public void child_removed(String path, Object payload) {
	JSONObject broadcast = new JSONObject();
	broadcast.put("type", "child_removed");
	broadcast.put("path", path);
	broadcast.put("payload", payload);
	roadrunnerSender.send(broadcast.toString());
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
