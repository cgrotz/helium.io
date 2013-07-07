package de.skiptag.roadrunner.direct;

import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import de.skiptag.roadrunner.authorization.AuthorizationService;
import de.skiptag.roadrunner.dataService.DataService;
import de.skiptag.roadrunner.dataService.DataService.QueryCallback;
import de.skiptag.roadrunner.messaging.DataListener;

public class RoadrunnerService implements DataListener {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RoadrunnerService.class);

    public interface SnapshotHandler {

	void handle(RoadrunnerSnapshot roadrunnerSnapshot);

    }

    private AuthorizationService authorizationService;
    private DataService dataService;

    private String path;
    private Multimap<String, SnapshotHandler> once = LinkedListMultimap.create();
    private Multimap<String, SnapshotHandler> on = LinkedListMultimap.create();
    private String contextName;

    public RoadrunnerService(AuthorizationService authorizationService,
	    DataService dataService, String contextName, String path) {
	this.authorizationService = authorizationService;
	this.dataService = dataService;
	dataService.addListener(this);
	this.contextName = contextName;
	this.path = path.replaceFirst("^[^#]*?://.*?(/.*)$", "");
	if (contextName != null) {
	    this.path = this.path.substring(contextName.length());
	}
    }

    public RoadrunnerService child(String childname) {
	return new RoadrunnerService(authorizationService, dataService,
		contextName, path + "/" + childname);
    }

    @Override
    public void child_added(String name, String path, String parent,
	    Object node, String prevChildName, boolean hasChildren,
	    long numChildren) {
	try {
	    RoadrunnerSnapshot roadrunnerSnapshot = new RoadrunnerSnapshot(
		    authorizationService, dataService, contextName, path, node,
		    parent, numChildren, prevChildName, hasChildren, 0);

	    if (on.containsKey("child_added")) {
		for (SnapshotHandler handler : on.get("child_added"))
		    handler.handle(roadrunnerSnapshot);
	    }
	    if (once.containsKey("child_added")) {
		for (SnapshotHandler handler : once.get("child_added"))
		    handler.handle(roadrunnerSnapshot);
		once.removeAll("child_added");
	    }
	} catch (Exception exp) {
	    logger.error("", exp);
	}
    }

    @Override
    public void child_changed(String name, String path, String parent,
	    Object node, String prevChildName, boolean hasChildren,
	    long numChildren) {
	try {
	    RoadrunnerSnapshot roadrunnerSnapshot = new RoadrunnerSnapshot(
		    authorizationService, dataService, contextName, path, node,
		    parent, numChildren, prevChildName, hasChildren, 0);
	    if (on.containsKey("child_changed")) {
		for (SnapshotHandler handler : on.get("child_changed"))
		    handler.handle(roadrunnerSnapshot);
	    }
	    if (once.containsKey("child_changed")) {
		for (SnapshotHandler handler : once.get("child_changed"))
		    handler.handle(roadrunnerSnapshot);
		once.removeAll("child_changed");
	    }
	} catch (Exception exp) {
	    logger.error("", exp);
	}
    }

    @Override
    public void child_moved(JSONObject childSnapshot, String prevChildName,
	    boolean hasChildren, long numChildren) {
	try {
	    RoadrunnerSnapshot roadrunnerSnapshot = new RoadrunnerSnapshot(
		    authorizationService, dataService, contextName, path,
		    childSnapshot, null, numChildren, prevChildName,
		    hasChildren, 0);
	    if (on.containsKey("child_moved")) {
		for (SnapshotHandler handler : on.get("child_moved"))
		    handler.handle(roadrunnerSnapshot);
	    }
	    if (once.containsKey("child_moved")) {
		for (SnapshotHandler handler : once.get("child_moved"))
		    handler.handle(roadrunnerSnapshot);
		once.removeAll("child_moved");
	    }
	} catch (Exception exp) {
	    logger.error("", exp);
	}
    }

    @Override
    public void child_removed(String path, Object payload) {
	try {
	    RoadrunnerSnapshot roadrunnerSnapshot = new RoadrunnerSnapshot(
		    authorizationService, dataService, contextName, path,
		    payload, null, 0, null, false, 0);
	    if (on.containsKey("child_removed")) {
		for (SnapshotHandler handler : on.get("child_removed"))
		    handler.handle(roadrunnerSnapshot);
	    }
	    if (once.containsKey("child_removed")) {
		for (SnapshotHandler handler : once.get("child_removed"))
		    handler.handle(roadrunnerSnapshot);
		once.removeAll("child_removed");
	    }
	} catch (Exception exp) {
	    logger.error("", exp);
	}
    }

    public String name() {
	return dataService.getName(path);
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
	return new RoadrunnerService(authorizationService, dataService,
		contextName, dataService.getParent(path));
    }

    public RoadrunnerService push(JSONObject data) {
	try {
	    String name = UUID.randomUUID().toString().replaceAll("-", "");
	    dataService.update((path.endsWith("/") ? path : path + "/") + name, data);

	    return new RoadrunnerService(authorizationService, dataService,
		    contextName, (path.endsWith("/") ? path : path + "/")
			    + name);
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    public void query(String expression, final SnapshotHandler handler) {
	dataService.query(expression, new QueryCallback() {
	    @Override
	    public void change(String path, JSONObject value,
		    String parentPath, long numChildren, String name,
		    boolean hasChildren, int priority) {
		RoadrunnerSnapshot snap = new RoadrunnerSnapshot(
			authorizationService, dataService, name, path, value,
			parentPath, numChildren, name, hasChildren, priority);
		handler.handle(snap);
	    }

	});
    }

    public RoadrunnerService root() {
	return new RoadrunnerService(authorizationService, dataService,
		contextName, "/");

    }

    public RoadrunnerService set(JSONObject data) {
	try {
	    dataService.update(path, data);
	    return this;
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    public RoadrunnerService update(JSONObject data) {
	try {
	    dataService.update(path, data);
	    return this;
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }
}
