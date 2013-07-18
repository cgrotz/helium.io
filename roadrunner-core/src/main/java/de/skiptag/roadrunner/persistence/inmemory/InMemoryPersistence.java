package de.skiptag.roadrunner.persistence.inmemory;

import java.util.Iterator;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class InMemoryPersistence implements Persistence {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryPersistence.class);

    private Node model = new Node();

    public InMemoryPersistence() {
    }

    @Override
    public Object get(Path path) {
	if (path == null || model.getObjectForPath(path) == null) {
	    return model;
	} else {
	    return model.getObjectForPath(path);
	}
    }

    @Override
    public void remove(Path path) {
	String nodeName = path.getLastElement();
	Path parentPath = path.getParent();
	model.getNodeForPath(parentPath).remove(nodeName);
    }

    @Override
    public void syncPath(Path path, RoadrunnerEventHandler handler) {

	Node node = model.getNodeForPath(path);

	Iterator<?> itr = node.keys();
	while (itr.hasNext()) {
	    Object childNodeKey = itr.next();
	    Object object = node.get(childNodeKey.toString());

	    handler.child_added((String) childNodeKey, path + "/"
		    + childNodeKey, path.toString(), object, null, (object instanceof Node) ? ((Node) object).hasChildren()
		    : false, (object instanceof Node) ? ((Node) object).length()
		    : 0);
	}
    }

    @Override
    public boolean applyNewValue(Path path, Object payload) {

	Node node;
	boolean created = false;
	if (!model.pathExists(path)) {
	    created = true;
	}
	if (payload instanceof JSONObject) {
	    node = model.getNodeForPath(path);
	    node.populate((JSONObject) payload);
	} else {
	    node = model.getNodeForPath(path.getParent());
	    node.put(path.getLastElement(), payload);
	}
	logger.trace("Model changed: " + model);
	return created;
    }

    @Override
    public JSONObject dumpSnapshot() {
	return model;
    }

    @Override
    public void restoreSnapshot(JSONObject payload) {
	model.populate(payload);
    }

}