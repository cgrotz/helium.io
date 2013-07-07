package de.skiptag.roadrunner.disruptor.processor.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.disruptor.event.MessageType;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.helper.Path;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.persistence.Persistence;

public class DistributionProcessor implements EventHandler<RoadrunnerEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DistributionProcessor.class);
    private RoadrunnerEventHandler handler;
    private Persistence persistence;

    public DistributionProcessor(Persistence persistence,
	    RoadrunnerEventHandler handler) {
	this.handler = handler;
	this.persistence = persistence;
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch)
	    throws Exception {
	Path path = new Path(event.extractNodePath());
	MessageType type = event.getType();
	Object node = persistence.get(path.toString());
	logger.trace("distributing event: " + event);
	if (type == MessageType.PUSH) {
	    handler.child_added((String) event.get("name"), path.toString()
		    + "/" + event.get("name"), path.getParent()
		    .getLastElement(), node, null, false, 0);
	} else if (type == MessageType.SET) {
	    if (event.has("payload") && !event.isNull("payload")) {
		handler.child_changed(path.getLastElement(), path.toString(), path.getParent()
			.getLastElement(), node, null, false, 0);
	    } else {
		handler.child_removed(path.toString(), event.getOldValue());
	    }
	}
    }
}
