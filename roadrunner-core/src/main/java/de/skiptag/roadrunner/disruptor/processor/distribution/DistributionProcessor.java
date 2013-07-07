package de.skiptag.roadrunner.disruptor.processor.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.dataService.DataService;
import de.skiptag.roadrunner.helper.Path;
import de.skiptag.roadrunner.disruptor.event.MessageType;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class DistributionProcessor implements EventHandler<RoadrunnerEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DistributionProcessor.class);

    private DataService dataService;

    public DistributionProcessor(DataService dataService) {
	this.dataService = dataService;
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch)
	    throws Exception {
	Path path = new Path(event.extractNodePath());
	MessageType type = event.getType();
	Object node = dataService.get(path.toString());
	logger.trace("distributing event: " + event);
	if (type == MessageType.PUSH) {
	    dataService.fireChildAdded((String) event.get("name"), path.toString()
		    + "/" + event.get("name"), path.getParent()
		    .getLastElement(), node, null, false, 0);
	} else if (type == MessageType.SET) {
	    if (event.has("payload") && !event.isNull("payload")) {
		dataService.fireChildChanged(path.getLastElement(), path.toString(), path.getParent()
			.getLastElement(), node, null, false, 0);
	    } else {
		dataService.fireChildRemoved(path.toString(), event.getOldValue());
	    }
	}
    }
}
