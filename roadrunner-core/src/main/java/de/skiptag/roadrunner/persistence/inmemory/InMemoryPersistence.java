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
    public Object get(String path) {
	return model.getObjectForPath(new Path(path));
    }

    @Override
    public String getName(String path) {
	return new Path(path).getLastElement();
    }

    @Override
    public String getParent(String path) {
	return new Path(path).getParent().toString();
    }

    @Override
    public void query(String expression, QueryCallback queryCallback) {

    }

    @Override
    public void remove(String path) {
	Path nodePath = new Path(path);
	String nodeName = nodePath.getLastElement();
	Path parentPath = nodePath.getParent();

	try {
	    model.getNodeForPath(parentPath).remove(nodeName);
	} catch (JSONException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void sync(String path, RoadrunnerEventHandler handler) {
	try {
	    Path nodePath = new Path(path);
	    Node node = model.getNodeForPath(nodePath);

	    Iterator<?> itr = node.sortedKeys();
	    while (itr.hasNext()) {
		Object childNodeKey = itr.next();
		Object object = node.get(childNodeKey.toString());

		{
		    handler.child_added((String) childNodeKey, path + "/"
			    + childNodeKey, nodePath.toString(), object, null, (object instanceof Node) ? ((Node) object).hasChildren()
			    : false, (object instanceof Node) ? ((Node) object).length()
			    : 0);
		}

	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void update(String nodeName, Object payload) {
	Path nodePath = new Path(nodeName);
	try {
	    Node node;
	    if (payload instanceof JSONObject) {
		node = model.getNodeForPath(nodePath);
		node.populate((JSONObject) payload);
	    } else {
		node = model.getNodeForPath(nodePath.getParent());
		node.put(nodePath.getLastElement(), payload);
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