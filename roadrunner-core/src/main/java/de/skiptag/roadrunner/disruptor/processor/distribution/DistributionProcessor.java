package de.skiptag.roadrunner.disruptor.processor.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.Node;

public class DistributionProcessor implements EventHandler<RoadrunnerEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DistributionProcessor.class);
    private RoadrunnerEventHandler handler;
    private Persistence persistence;

    public DistributionProcessor(Persistence persistence,
	    Authorization authorization, RoadrunnerEventHandler handler) {
	this.handler = handler;
	this.persistence = persistence;
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch) {
	logger.trace("distributing event: " + event);

	String path = event.extractNodePath();

	Path nodePath = new Path(event.extractNodePath());
	RoadrunnerEventType type = event.getType();
	Object node = persistence.get(nodePath);
	if (type == RoadrunnerEventType.PUSH) {
	    handler.child_added((String) event.get("name"), path, nodePath.getParent()
		    .getLastElement(), node, hasChildren(node), childCount(node));
	} else if (type == RoadrunnerEventType.SET) {
	    if (event.has("payload") && !event.isNull("payload")) {
		if (event.created()) {
		    handler.child_added(nodePath.getLastElement(), path, nodePath.getParent()
			    .getLastElement(), node, hasChildren(node), childCount(node));
		} else {
		    handler.child_changed(nodePath.getLastElement(), path, nodePath.getParent()
			    .getLastElement(), node, hasChildren(node), childCount(node));
		}
	    } else {
		handler.child_removed(path, event.getOldValue());
	    }
	}
    }

    private long childCount(Object node) {
	return (node instanceof Node) ? ((Node) node).getChildren().size() : 0;
    }

    private boolean hasChildren(Object node) {
	return (node instanceof Node) ? ((Node) node).hasChildren() : false;
    }
}
