package de.skiptag.roadrunner.api;

import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.messaging.DataListener;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class RoadrunnerService implements DataListener {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RoadrunnerService.class);

    public interface SnapshotHandler {
	void handle(RoadrunnerSnapshot roadrunnerSnapshot);
    }

    private Authorization authorization;
    private Persistence persistence;

    private String path;
    private Multimap<String, SnapshotHandler> once = LinkedListMultimap.create();
    private Multimap<String, SnapshotHandler> on = LinkedListMultimap.create();
    private String contextName;

    public RoadrunnerService(Authorization authorization,
	    Persistence persistence, String contextName, String path) {
	this.authorization = authorization;
	this.persistence = persistence;
	this.contextName = contextName;
	this.path = path.replaceFirst("^[^#]*?://.*?(/.*)$", "");
	if (contextName != null) {
	    this.path = this.path.substring(contextName.length());
	}
    }

    public RoadrunnerService child(String childname) {
	return new RoadrunnerService(authorization, persistence, contextName,
		path + "/" + childname);
    }

    @Override
    public void child_added(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren) {
	RoadrunnerSnapshot roadrunnerSnapshot = new RoadrunnerSnapshot(
		authorization, persistence, contextName, path, node, parent,
		numChildren, hasChildren);

	if (on.containsKey("child_added")) {
	    for (SnapshotHandler handler : on.get("child_added"))
		handler.handle(roadrunnerSnapshot);
	}
	if (once.containsKey("child_added")) {
	    for (SnapshotHandler handler : once.get("child_added"))
		handler.handle(roadrunnerSnapshot);
	    once.removeAll("child_added");
	}
    }

    @Override
    public void child_changed(String name, String path, String parent,
	    Object node, boolean hasChildren, long numChildren) {
	RoadrunnerSnapshot roadrunnerSnapshot = new RoadrunnerSnapshot(
		authorization, persistence, contextName, path, node, parent,
		numChildren, hasChildren);
	if (on.containsKey("child_changed")) {
	    for (SnapshotHandler handler : on.get("child_changed"))
		handler.handle(roadrunnerSnapshot);
	}
	if (once.containsKey("child_changed")) {
	    for (SnapshotHandler handler : once.get("child_changed"))
		handler.handle(roadrunnerSnapshot);
	    once.removeAll("child_changed");
	}
    }

    @Override
    public void child_moved(JSONObject childSnapshot, boolean hasChildren,
	    long numChildren) {

	RoadrunnerSnapshot roadrunnerSnapshot = new RoadrunnerSnapshot(
		authorization, persistence, contextName, path, childSnapshot,
		null, numChildren, hasChildren);
	if (on.containsKey("child_moved")) {
	    for (SnapshotHandler handler : on.get("child_moved"))
		handler.handle(roadrunnerSnapshot);
	}
	if (once.containsKey("child_moved")) {
	    for (SnapshotHandler handler : once.get("child_moved"))
		handler.handle(roadrunnerSnapshot);
	    once.removeAll("child_moved");
	}
    }

    @Override
    public void child_removed(String path, Object payload) {

	RoadrunnerSnapshot roadrunnerSnapshot = new RoadrunnerSnapshot(
		authorization, persistence, contextName, path, payload, null,
		0, false);
	if (on.containsKey("child_removed")) {
	    for (SnapshotHandler handler : on.get("child_removed"))
		handler.handle(roadrunnerSnapshot);
	}
	if (once.containsKey("child_removed")) {
	    for (SnapshotHandler handler : once.get("child_removed"))
		handler.handle(roadrunnerSnapshot);
	    once.removeAll("child_removed");
	}
    }

    public String name() {
	return new Path(path).getLastElement();
    }

    public void off(String eventType, SnapshotHandler handler) {
	on.remove(handler, handler);
	once.remove(handler, handler);
    }

    public void on(String eventType, SnapshotHandler handler) {
	on.put(eventType, handler);
    }

    public void once(String eventType, SnapshotHandler handler) {
	once.put(eventType, handler);
    }

    public RoadrunnerService parent() {
	return new RoadrunnerService(authorization, persistence, contextName,
		new Path(path).getParent().toString());
    }

    public RoadrunnerService push(JSONObject data) {

	String name = UUID.randomUUID().toString().replaceAll("-", "");
	persistence.applyNewValue(new Path((path.endsWith("/") ? path : path
		+ "/")
		+ name), data);

	return new RoadrunnerService(authorization, persistence, contextName,
		(path.endsWith("/") ? path : path + "/") + name);
    }

    public RoadrunnerService root() {
	return new RoadrunnerService(authorization, persistence, contextName,
		"/");

    }

    public RoadrunnerService set(Object data) {
	boolean created = persistence.applyNewValue(new Path(path), data);
	return this;
    }

    public RoadrunnerService update(Object data) {
	persistence.applyNewValue(new Path(path), data);
	return this;
    }
}
