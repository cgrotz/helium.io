package de.skiptag.roadrunner.messaging;

import java.util.Set;

import org.json.JSONObject;

import com.google.common.collect.Sets;

public class RoadrunnerEventHandler implements DataListener {

    private RoadrunnerSender roadrunnerSender;

    private Set<String> attached_listeners = Sets.newHashSet();

    public RoadrunnerEventHandler(RoadrunnerSender roadrunnerSender) {
	this.roadrunnerSender = roadrunnerSender;
    }

    @Override
    public void child_added(String name, String path, String parent,
	    Object node, String prevChildName, boolean hasChildren,
	    long numChildren) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_added");
	    broadcast.put("name", name);
	    broadcast.put("path", getPath(path));
	    broadcast.put("parent", parent);
	    broadcast.put("payload", node);
	    broadcast.put("prevChildName", prevChildName);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    if (listenerAttached(path)) {
		roadrunnerSender.send(broadcast.toString());
	    }
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override
    public void child_changed(String name, String path, String parent,
	    Object node, String prevChildName, boolean hasChildren,
	    long numChildren) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_changed");
	    broadcast.put("name", name);
	    broadcast.put("path", getPath(path));
	    broadcast.put("parent", parent);
	    broadcast.put("payload", node);
	    broadcast.put("prevChildName", prevChildName);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    roadrunnerSender.send(broadcast.toString());
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override
    public void child_moved(JSONObject childSnapshot, String prevChildName,
	    boolean hasChildren, long numChildren) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_moved");
	    broadcast.put("payload", childSnapshot);
	    broadcast.put("prevChildName", prevChildName);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    roadrunnerSender.send(broadcast.toString());
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override
    public void child_removed(String path, Object payload) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_removed");
	    broadcast.put("path", getPath(path));
	    broadcast.put("payload", payload);
	    roadrunnerSender.send(broadcast.toString());
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    public void addListener(String path) {
	attached_listeners.add(path);

    }

    public void removeListener(String path) {
	attached_listeners.remove(path);

    }

    private boolean listenerAttached(String path) {
	for (String listenerPath : attached_listeners) {
	    if (path.startsWith(listenerPath)) {
		return true;
	    }
	}
	return false;
    }

    private String getPath(String path) {
	String workpath = path;
	// workpath = workpath.replaceAll("//", "/");
	// if (workpath.startsWith("/" + repoName)) {
	// workpath = workpath.substring(("/" + repoName).length() + 1);
	// }
	// workpath = workpath.startsWith("/") ? workpath : ("/" + workpath);
	return workpath;
    }
}
