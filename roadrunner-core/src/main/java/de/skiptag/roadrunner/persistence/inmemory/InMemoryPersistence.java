package de.skiptag.roadrunner.persistence.inmemory;

import org.json.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;
import de.skiptag.roadrunner.disruptor.event.changelog.ChangeLog;
import de.skiptag.roadrunner.disruptor.event.changelog.ChangeLogBuilder;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.queries.QueryEvaluator;

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
    public void remove(ChangeLog log, Path path) {
	String nodeName = path.getLastElement();
	Path parentPath = path.getParent();
	Node node = model.getNodeForPath(parentPath).getNode(nodeName);
	model.getNodeForPath(parentPath).remove(nodeName);
	log.addChildRemovedLogEntry(parentPath, nodeName, node);
    }

    @Override
    public void syncPath(Path path, RoadrunnerEndpoint handler) {
	Node node = model.getNodeForPath(path);

	for (String childNodeKey : node.keys()) {
	    Object object = node.get(childNodeKey);
	    boolean hasChildren = (object instanceof Node) ? ((Node) object).hasChildren()
		    : false;
	    int indexOf = node.indexOf(childNodeKey);
	    int numChildren = (object instanceof Node) ? ((Node) object).length()
		    : 0;
	    if (object != null && object != Node.NULL) {
		handler.fireChildAdded(childNodeKey, path, path.getParent(), object, hasChildren, numChildren, null, indexOf);
	    }
	}
    }

    public void syncPathWithQuery(Path path, RoadrunnerEndpoint handler,
	    QueryEvaluator queryEvaluator, String query) {
	Node node = model.getNodeForPath(path);

	for (String childNodeKey : node.keys()) {
	    Object object = node.get(childNodeKey);
	    if (queryEvaluator.evaluateQueryOnValue(object, query)) {
		boolean hasChildren = (object instanceof Node) ? ((Node) object).hasChildren()
			: false;
		int indexOf = node.indexOf(childNodeKey);
		int numChildren = (object instanceof Node) ? ((Node) object).length()
			: 0;
		if (object != null && object != Node.NULL) {
		    handler.fireQueryChildAdded(childNodeKey, path, path.getParent(), object, hasChildren, numChildren, null, indexOf);
		}
	    }
	}
    }

    @Override
    public void syncPropertyValue(Path path, RoadrunnerEndpoint handler) {
	Node node = model.getNodeForPath(path.getParent());
	String childNodeKey = path.getLastElement();
	if (node.has(path.getLastElement())) {
	    Object object = node.get(path.getLastElement());
	    handler.fireValue(childNodeKey, path, path.getParent(), object, "", node.indexOf(childNodeKey));
	} else {
	    handler.fireValue(childNodeKey, path, path.getParent(), "", "", node.indexOf(childNodeKey));
	}
    }

    @Override
    public void applyNewValue(ChangeLog log, Path path, int priority,
	    Object payload) {
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
	    node.populate(new ChangeLogBuilder(log, path, path.getParent(),
		    node), (Node) payload);
	    if (created) {
		log.addChildAddedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
			.getParent(), payload, false, 0, prevChildName(parent, priority(parent, path.getLastElement())), priority(parent, path.getLastElement()));
	    } else {
		log.addChildChangedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
			.getParent(), payload, false, 0, prevChildName(parent, priority(parent, path.getLastElement())), priority(parent, path.getLastElement()));
	    }
	} else {
	    parent.putWithIndex(path.getLastElement(), payload, priority);

	    if (created) {
		log.addChildAddedLogEntry(path.getLastElement(), path, path.getParent(), payload, false, 0, prevChildName(parent, priority(parent, path.getLastElement())), priority(parent, path.getLastElement()));
	    } else {
		log.addChildChangedLogEntry(path.getLastElement(), path.getParent(), path.getParent()
			.getParent(), payload, false, 0, prevChildName(parent, priority(parent, path.getLastElement())), priority(parent, path.getLastElement()));
		log.addValueChangedLogEntry(path.getLastElement(), path, path.getParent(), payload, prevChildName(parent, priority(parent, path.getLastElement())), priority(parent, path.getLastElement()));
	    }
	    log.addChildChangedLogEntry(path.getParent().getLastElement(), path.getParent()
		    .getParent(), path.getParent().getParent().getParent(), parent, false, 0, prevChildName(parent, priority(parent, path.getLastElement())), priority(parent, path.getLastElement()));

	}
	logger.trace("Model changed: " + model);
    }

    @Override
    public void setPriority(ChangeLog log, Path path, int priority) {
	Node parent = model.getNodeForPath(path.getParent());
	parent.setIndexOf(path.getLastElement(), priority);
    }

    @Override
    public Node dumpSnapshot() {
	return model;
    }

    @Override
    public void restoreSnapshot(Node node) {
	model.populate(null, node);
    }

    private String prevChildName(Node parent, int priority) {
	if (priority <= 0) {
	    return null;
	}
	return parent.keys().get(priority - 1);
    }

    private long childCount(Object node) {
	return (node instanceof Node) ? ((Node) node).getChildren().size() : 0;
    }

    private int priority(Node parentNode, String name) {
	return parentNode.indexOf(name);
    }

    private boolean hasChildren(Object node) {
	return (node instanceof Node) ? ((Node) node).hasChildren() : false;
    }

    @Override
    public RulesDataSnapshot getRoot() {
	return new InMemoryDataSnapshot(model);
    }
}