package de.skiptag.roadrunner.dataService.inmemory;

import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.skiptag.roadrunner.authorization.AuthorizationService;
import de.skiptag.roadrunner.dataService.DataService;
import de.skiptag.roadrunner.helper.Path;
import de.skiptag.roadrunner.messaging.DataListener;

public class InMemoryDataService implements DataService {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryDataService.class);

    private AuthorizationService authorizationService;

    private Node model = new Node();
    private Set<DataListener> listeners = Sets.newHashSet();

    public InMemoryDataService(AuthorizationService authorizationService) {
	this.authorizationService = authorizationService;
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
    public void sync(String path) {
	try {
	    Path nodePath = new Path(path);
	    Node node = model.getNodeForPath(nodePath);

	    Iterator<?> itr = node.sortedKeys();
	    while (itr.hasNext()) {
		Object childNodeKey = itr.next();
		Object object = node.get(childNodeKey.toString());

		{
		    fireChildAdded((String) childNodeKey, path + "/"
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
    public void addListener(DataListener dataListener) {
	this.listeners.add(dataListener);
    }

    @Override
    public void removeListener(DataListener dataListener) {
	this.listeners.remove(dataListener);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void setAuth(JSONObject auth) {
	// TODO Auto-generated method stub

    }

    @Override
    public void fireChildAdded(String name, String path, String parentName,
	    Object payload, String prevChildName, boolean hasNodes, long size) {
	for (DataListener listener : Sets.newHashSet(listeners)) {
	    listener.child_added(name, path, parentName, payload, prevChildName, hasNodes, size);
	}
    }

    @Override
    public void fireChildChanged(String name, String path, String parentName,
	    Object payload, String prevChildName, boolean hasNodes, long size) {
	for (DataListener listener : Sets.newHashSet(listeners)) {
	    listener.child_changed(name, path, parentName, payload, prevChildName, hasNodes, size);
	}
    }

    @Override
    public void fireChildMoved(JSONObject childSnapshot, String prevChildName,
	    boolean hasNodes, long size) {
	for (DataListener listener : Sets.newHashSet(listeners)) {
	    listener.child_moved(childSnapshot, prevChildName, hasNodes, size);
	}
    }

    @Override
    public void fireChildRemoved(String path, JSONObject fromRemovedNodes) {
	for (DataListener listener : Sets.newHashSet(listeners)) {
	    listener.child_removed(path, fromRemovedNodes);
	}
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