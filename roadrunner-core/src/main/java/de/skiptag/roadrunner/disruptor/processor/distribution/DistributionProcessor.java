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

	String path = event.extractNodePath();

	Path nodePath = new Path(event.extractNodePath());
	RoadrunnerEventType type = event.getType();
	if (type == RoadrunnerEventType.PUSH) {
	    Object node = persistence.get(nodePath);
	    fireChildAdded((String) event.get("name"), path, nodePath.getParent()
		    .getLastElement(), node, hasChildren(node), childCount(node));
	} else if (type == RoadrunnerEventType.SET) {
	    if (event.has("payload") && !event.isNull("payload")) {
		if (event.created()) {
		    Object node = persistence.get(nodePath);
		    fireChildAdded(nodePath.getLastElement(), path, nodePath.getParent()
			    .getLastElement(), node, hasChildren(node), childCount(node));
		} else {
		    Object node = persistence.get(nodePath);
		    fireChildChanged(nodePath.getLastElement(), path, nodePath.getParent()
			    .getLastElement(), node, hasChildren(node), childCount(node));
		}
	    } else {
		firChildRemoved(path, event.getOldValue());
	    }
	}

	org.joda.time.Duration dur = new org.joda.time.Duration(
		event.getCreationDate(), System.currentTimeMillis());
	logger.trace("Message Processing took: " + dur.getMillis() + "ms");
    }

    private void firChildRemoved(String path, JSONObject payload) {
	for (DataListener handler : handlers) {
	    handler.child_removed(path, payload);
	}
    }

    private void fireChildChanged(String lastElement, String path,
	    String lastElement2, Object node, boolean hasChildren,
	    long childCount) {
	for (DataListener handler : handlers) {
	    handler.child_changed(lastElement, path, lastElement2, node, hasChildren, childCount);
	}
    }

    private void fireChildAdded(String string, String path, String lastElement,
	    Object node, boolean hasChildren, long childCount) {
	for (DataListener handler : handlers) {
	    handler.child_added(string, path, lastElement, node, hasChildren, childCount);
	}
    }

    private long childCount(Object node) {
	return (node instanceof Node) ? ((Node) node).getChildren().size() : 0;
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
