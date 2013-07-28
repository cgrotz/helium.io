package de.skiptag.roadrunner.disruptor.processor.distribution;

import java.util.Set;

import org.json.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.messaging.DataListener;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class DistributionProcessor implements EventHandler<RoadrunnerEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DistributionProcessor.class);
    private Persistence persistence;

    private Set<DataListener> handlers = Sets.newHashSet();

    public DistributionProcessor(Persistence persistence,
	    Authorization authorization) {
	this.persistence = persistence;
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch) {
	logger.trace("distributing event: " + event);
	if (!event.isFromHistory()) {
	    distribute(event);
	}
    }

    public void distribute(RoadrunnerEvent event) {
	String path = event.extractNodePath();
	String pathToParent = event.extractParentPath();

	Path nodePath = new Path(event.extractNodePath());
	RoadrunnerEventType type = event.getType();
	Path parentPath = nodePath.getParent();
	if (type == RoadrunnerEventType.PUSH) {
	    Node parent = persistence.getNode(parentPath);
	    Object value = parent.get(nodePath.getLastElement());
	    int priority = priority((Node) persistence.get(parentPath), nodePath.getLastElement());
	    fireChildAdded((String) event.get(RoadrunnerEvent.NAME), pathToParent, parentPath.getLastElement(), value, hasChildren(value), childCount(value), prevChildName(parent, priority), priority);
	} else if (type == RoadrunnerEventType.SET) {
	    if (event.has(RoadrunnerEvent.PAYLOAD)
		    && !event.isNull(RoadrunnerEvent.PAYLOAD)) {
		Node parent = persistence.getNode(parentPath);
		Object value = parent.get(nodePath.getLastElement());
		int priority = priority((Node) persistence.get(parentPath), nodePath.getLastElement());

		if (event.created()) {
		    fireChildAdded(nodePath.getLastElement(), pathToParent, parentPath.getLastElement(), value, hasChildren(value), childCount(value), prevChildName(parent, priority), priority);
		} else {
		    fireChildChanged(nodePath.getLastElement(), path, parentPath.getLastElement(), value, hasChildren(value), childCount(value), prevChildName(parent, priority), priority);

		    int parentPriority = priority(persistence.getNode(parentPath.getParent()), parentPath.getLastElement());
		    fireChildChanged(parentPath.getLastElement(), pathToParent, parentPath.getParent()
			    .getLastElement(), parent, hasChildren(parent), childCount(parent), prevChildName(persistence.getNode(parentPath.getParent()), parentPriority), parentPriority);
		}

		fireValue(nodePath.getLastElement(), path, parentPath.getLastElement(), value, false, 0, prevChildName(parent, priority), priority);
	    } else {
		firChildRemoved(pathToParent, nodePath.getLastElement(), event.getOldValue());
	    }
	} else if (type == RoadrunnerEventType.EVENT) {
	    for (DataListener handler : Sets.newHashSet(handlers)) {
		Node jsonObject;
		Object object = event.get(RoadrunnerEvent.PAYLOAD);
		if (object instanceof Node) {
		    jsonObject = event.getNode(RoadrunnerEvent.PAYLOAD);
		    handler.distributeEvent(path, jsonObject);
		} else if (object instanceof String) {
		    jsonObject = new Node(RoadrunnerEvent.PAYLOAD);
		    handler.distributeEvent(path, new Node((String) object));
		}
	    }
	} else if (type == RoadrunnerEventType.SETPRIORITY) {

	}

	org.joda.time.Duration dur = new org.joda.time.Duration(
		event.getCreationDate(), System.currentTimeMillis());
	logger.trace("Message Processing took: " + dur.getMillis() + "ms");
    }

    private String prevChildName(Node parent, int priority) {
	if (priority <= 0) {
	    return null;
	}
	return parent.keys().get(priority - 1);
    }

    private void firChildRemoved(String path, String name, Node payload) {
	for (DataListener handler : Sets.newHashSet(handlers)) {
	    handler.child_removed(path, name, payload);
	}
    }

    private void fireChildChanged(String lastElement, String path,
	    String lastElement2, Object node, boolean hasChildren,
	    long childCount, String prevChildName, int priority) {
	if (node != null && node != Node.NULL) {
	    for (DataListener handler : Sets.newHashSet(handlers)) {
		handler.child_changed(lastElement, path, lastElement2, node, hasChildren, childCount, priority);
	    }
	}
    }

    private void fireValue(String lastElement, String path,
	    String lastElement2, Object node, boolean hasChildren,
	    long childCount, String prevChildName, int priority) {
	for (DataListener handler : Sets.newHashSet(handlers)) {
	    handler.value(lastElement, path, lastElement2, node, hasChildren, childCount, priority);
	}
    }

    private void fireChildAdded(String string, String path, String lastElement,
	    Object node, boolean hasChildren, long childCount,
	    String prevChildName, int priority) {
	if (node != null && node != Node.NULL) {
	    for (DataListener handler : Sets.newHashSet(handlers)) {
		handler.child_added(string, path, lastElement, node, hasChildren, childCount, priority);
	    }
	}
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

    public void addHandler(DataListener handler) {
	handlers.add(handler);
    }

    public void removeHandler(DataListener handler) {
	handlers.remove(handler);
    }
}
