package de.skiptag.roadrunner.persistence.inmemory;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class InMemoryPersistence implements Persistence {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryPersistence.class);

    private Authorization authorization;

    private Node model = new Node();

    private JSONObject auth;

    public InMemoryPersistence(Authorization authorization) {
	this.authorization = authorization;
    }

    @Override
    public Object get(Path path) {
	return model.getObjectForPath(path);
    }

    @Override
    public String getName(Path path) {
	return path.getLastElement();
    }

    @Override
    public String getParent(Path path) {
	return path.getParent().toString();
    }

    @Override
    public void query(String expression, QueryCallback queryCallback) {

    }

    @Override
    public void remove(Path path) {
	String nodeName = path.getLastElement();
	Path parentPath = path.getParent();

	try {
	    model.getNodeForPath(parentPath).remove(nodeName);
	} catch (JSONException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void sync(Path path, RoadrunnerEventHandler handler) {
	try {
	    Node node = model.getNodeForPath(path);

	    Iterator<?> itr = node.sortedKeys();
	    while (itr.hasNext()) {
		Object childNodeKey = itr.next();
		Object object = node.get(childNodeKey.toString());

		{
		    handler.child_added((String) childNodeKey, path + "/"
			    + childNodeKey, path.toString(), object, null, (object instanceof Node) ? ((Node) object).hasChildren()
			    : false, (object instanceof Node) ? ((Node) object).length()
			    : 0);
		}

	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void update(Path path, Object payload) {
	try {
	    Node node;
	    if (payload instanceof JSONObject) {
		node = model.getNodeForPath(path);
		node.populate((JSONObject) payload);
	    } else {
		node = model.getNodeForPath(path.getParent());
		node.put(path.getLastElement(), payload);
	    }
	    logger.trace("Model changed: " + model);
	} catch (JSONException e) {

	    logger.error("", e);
	}
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void setAuth(JSONObject auth) {
	this.auth = auth;
    }

    @Override
    public JSONObject dumpSnapshot() {
	return model;
    }

    @Override
    public void restoreSnapshot(JSONObject payload) throws JSONException {
	model.populate(payload);
    }

}