package de.skiptag.roadrunner.disruptor.processor.distribution;

import java.util.Set;

import org.json.JSONObject;
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
import de.skiptag.roadrunner.persistence.inmemory.Node;

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

	Path nodePath = new Path(event.extractNodePath());
	RoadrunnerEventType type = event.getType();
	if (type == RoadrunnerEventType.PUSH) {
	    Node parent = persistence.getNode(nodePath.getParent());
	    Object node = parent.get(nodePath.getLastElement());
	    int priority = priority((Node) persistence.get(nodePath.getParent()), nodePath.getLastElement());
	    fireChildAdded((String) event.get(RoadrunnerEvent.NAME), path, nodePath.getParent()
		    .getLastElement(), node, hasChildren(node), childCount(node), prevChildName(parent, priority), priority);
	} else if (type == RoadrunnerEventType.SET) {
	    if (event.has(RoadrunnerEvent.PAYLOAD)
		    && !event.isNull(RoadrunnerEvent.PAYLOAD)) {
		if (event.created()) {
		    Node parent = persistence.getNode(nodePath.getParent());
		    Object node = parent.get(nodePath.getLastElement());
		    int priority = priority((Node) persistence.get(nodePath.getParent()), nodePath.getLastElement());
		    fireChildAdded(nodePath.getLastElement(), path, nodePath.getParent()
			    .getLastElement(), node, hasChildren(node), childCount(node), prevChildName(parent, priority), priority);
		} else {
		    Node parent = persistence.getNode(nodePath.getParent());
		    Object value = parent.get(nodePath.getLastElement());
		    int priority = priority((Node) persistence.get(nodePath.getParent()), nodePath.getLastElement());
		    fireChildChanged(nodePath.getLastElement(), path, nodePath.getParent()
			    .getLastElement(), value, hasChildren(value), childCount(value), prevChildName(parent, priority), priority);
		    if (!(value instanceof Node)) {
			fireValue(nodePath.getLastElement(), path, nodePath.getParent()
				.getLastElement(), value, false, 0, prevChildName(parent, priority), priority);
		    }
		}
	    } else {
		firChildRemoved(path, event.getOldValue());
	    }
	} else if (type == RoadrunnerEventType.EVENT) {
	    for (DataListener handler : Sets.newHashSet(handlers)) {
		JSONObject jsonObject;
		Object object = event.get(RoadrunnerEvent.PAYLOAD);
		if (object instanceof JSONObject) {
		    jsonObject = event.getJSONObject(RoadrunnerEvent.PAYLOAD);
		    handler.distributeEvent(path, jsonObject);
		} else if (object instanceof String) {
		    jsonObject = new JSONObject(RoadrunnerEvent.PAYLOAD);
		    handler.distributeEvent(path, new JSONObject(
			    (String) object));
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

    private void firChildRemoved(String path, JSONObject payload) {
	for (DataListener handler : Sets.newHashSet(handlers)) {
	    handler.child_removed(path, payload);
	}
    }

    private void fireChildChanged(String lastElement, String path,
	    String lastElement2, Object node, boolean hasChildren,
	    long childCount, String prevChildName, int priority) {
	for (DataListener handler : Sets.newHashSet(handlers)) {
	    handler.child_changed(lastElement, path, lastElement2, node, hasChildren, childCount, priority);
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
	for (DataListener handler : Sets.newHashSet(handlers)) {
	    handler.child_added(string, path, lastElement, node, hasChildren, childCount, priority);
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
