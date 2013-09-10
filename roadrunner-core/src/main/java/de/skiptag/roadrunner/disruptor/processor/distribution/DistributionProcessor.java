package de.skiptag.roadrunner.disruptor.processor.distribution;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.common.Path;
import de.skiptag.roadrunner.event.RoadrunnerEvent;
import de.skiptag.roadrunner.json.Node;
import de.skiptag.roadrunner.messaging.RoadrunnerSocket;

public class DistributionProcessor implements EventHandler<RoadrunnerEvent> {

	private static final Logger		logger		= LoggerFactory.getLogger(DistributionProcessor.class);

	private Set<RoadrunnerSocket>	handlers	= Sets.newHashSet();

	private long									sequence;

	@Override
	public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch) {
		this.sequence = sequence;
		distribute(event);
	}

	public void distribute(RoadrunnerEvent event) {
		logger.trace("distributing event: " + event);
		if (!event.isFromHistory()) {
			for (RoadrunnerSocket handler : Sets.newHashSet(handlers)) {
				handler.distribute(event);
			}
		}
	}

	public void distribute(String path, Node data) {
		for (RoadrunnerSocket handler : Sets.newHashSet(handlers)) {
			handler.distributeEvent(new Path(RoadrunnerEvent.extractPath(path)), data);
		}
	}

	public void addHandler(RoadrunnerSocket handler) {
		handlers.add(handler);
	}

	public void removeHandler(RoadrunnerSocket handler) {
		handlers.remove(handler);
	}

	public long getSequence() {
		return sequence;
	}
}
