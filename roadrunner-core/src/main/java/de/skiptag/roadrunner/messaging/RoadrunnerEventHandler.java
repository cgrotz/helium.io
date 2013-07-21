package de.skiptag.roadrunner.messaging;

import java.util.Set;

import org.json.JSONObject;

import com.google.common.collect.Sets;

public class RoadrunnerEventHandler implements DataListener {

    private Set<RoadrunnerResponseSender> senders = Sets.newHashSet();

    private Set<String> attached_listeners = Sets.newHashSet();

    @Override
    public void child_added(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren) {
	if (hasListener(path)) {
	    for (RoadrunnerResponseSender sender : senders) {
		JSONObject broadcast = new JSONObject();
		broadcast.put("type", "child_added");
		broadcast.put("name", name);
		broadcast.put("path", sender.getBasePath() + path);
		broadcast.put("parent", parent);
		broadcast.put("payload", node);
		broadcast.put("hasChildren", hasChildren);
		broadcast.put("numChildren", numChildren);
		sender.send(broadcast.toString());
	    }
	}
    }

    @Override
    public void child_changed(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren) {
	if (hasListener(path)) {
	    for (RoadrunnerResponseSender sender : senders) {
		JSONObject broadcast = new JSONObject();
		broadcast.put("type", "child_changed");
		broadcast.put("name", name);
		broadcast.put("path", sender.getBasePath() + path);
		broadcast.put("parent", parent);
		broadcast.put("payload", node);
		broadcast.put("hasChildren", hasChildren);
		broadcast.put("numChildren", numChildren);
		sender.send(broadcast.toString());
	    }
	}
    }

    @Override
    public void child_moved(JSONObject childSnapshot, boolean hasChildren,
	    long numChildren) {
	for (RoadrunnerResponseSender sender : senders) {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_moved");
	    broadcast.put("payload", childSnapshot);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    sender.send(broadcast.toString());
	}
    }

    @Override
    public void child_removed(String path, Object payload) {
	if (hasListener(path)) {
	    for (RoadrunnerResponseSender sender : senders) {
		JSONObject broadcast = new JSONObject();
		broadcast.put("type", "child_removed");
		broadcast.put("path", sender.getBasePath() + path);
		broadcast.put("payload", payload);

		sender.send(broadcast.toString());
	    }
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

    public void addSender(RoadrunnerResponseSender roadrunnerSender) {
	senders.add(roadrunnerSender);
    }

    public void removeSender(RoadrunnerResponseSender roadrunnerSender) {
	senders.remove(roadrunnerSender);
    }
}
