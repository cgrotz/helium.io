package de.skiptag.roadrunner.persistence.inmemory;

import org.json.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class InMemoryPersistence implements Persistence {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryPersistence.class);

    private Node model = new Node();

    @Override
    public Object get(Path path) {
	if (path == null || model.getObjectForPath(path) == null) {
	    return model;
	} else {
	    return model.getObjectForPath(path);
	}
    }

    @Override
    public Node getNode(Path path) {
	return model.getNodeForPath(path);
    }

    @Override
    public void remove(Path path) {
	String nodeName = path.getLastElement();
	Path parentPath = path.getParent();
	model.getNodeForPath(parentPath).remove(nodeName);
    }

    @Override
    public void syncPath(Path path, RoadrunnerEndpoint handler) {
	Node node = model.getNodeForPath(path);

	for (String childNodeKey : node.keys()) {
	    Object object = node.get(childNodeKey);
	    String pathForReturn = path.toString();
	    String parentName = path.toString();
	    boolean hasChildren = (object instanceof Node) ? ((Node) object).hasChildren()
		    : false;
	    int indexOf = node.indexOf(childNodeKey);
	    int numChildren = (object instanceof Node) ? ((Node) object).length()
		    : 0;
	    if (object != null && object != Node.NULL) {
		handler.child_added(childNodeKey, pathForReturn, parentName, object, hasChildren, numChildren, indexOf);
	    }
	}
    }

    @Override
    public void syncPropertyValue(Path path, RoadrunnerEndpoint handler) {
	Node node = model.getNodeForPath(path.getParent());
	String childNodeKey = path.getLastElement();
	if (node.has(path.getLastElement())) {
	    Object object = node.get(path.getLastElement());
	    handler.value(childNodeKey, path.toString(), path.getParent()
		    .toString(), object, false, 0L, node.indexOf(childNodeKey));
	} else {
	    handler.value(childNodeKey, path.toString(), path.getParent()
		    .toString(), "", false, 0L, node.indexOf(childNodeKey));
	}
    }

    @Override
    public boolean applyNewValue(Path path, int priority, Object payload) {
	Node node;
	boolean created = false;
	if (!model.pathExists(path)) {
	    created = true;
	}
	Node parent = model.getNodeForPath(path.getParent());
	if (payload instanceof Node) {
	    if (parent.has(path.getLastElement())) {
		node = parent.getNode(path.getLastElement());
		parent.setIndexOf(path.getLastElement(), priority);
	    } else {
		node = new Node();
		parent.putWithIndex(path.getLastElement(), node, priority);
	    }
	    node.populate((Node) payload);
	} else {
	    parent.putWithIndex(path.getLastElement(), payload, priority);
	}
	logger.trace("Model changed: " + model);
	return created;
    }

    @Override
    public void setPriority(Path path, int priority) {
	Node parent = model.getNodeForPath(path.getParent());
	parent.setIndexOf(path.getLastElement(), priority);
    }

    @Override
    public Node dumpSnapshot() {
	return model;
    }

    @Override
    public void restoreSnapshot(Node node) {
	model.populate(node);
    }

}