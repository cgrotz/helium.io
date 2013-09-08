package de.skiptag.roadrunner.disruptor.processor.distribution;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.event.RoadrunnerEvent;
import de.skiptag.roadrunner.messaging.DataListener;

public class DistributionProcessor implements EventHandler<RoadrunnerEvent> {

	private static final Logger logger = LoggerFactory
			.getLogger(DistributionProcessor.class);

	private Set<DataListener> handlers = Sets.newHashSet();

	private long sequence;

	@Override
	public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch) {
		this.sequence = sequence;
		distribute(event);
	}

	public void distribute(RoadrunnerEvent event) {
		logger.trace("distributing event: " + event);
		if (!event.isFromHistory()) {
			for (DataListener handler : Sets.newHashSet(handlers)) {
				handler.distribute(event);
			}
		}
	}

	public void addHandler(DataListener handler) {
		handlers.add(handler);
	}

	public void removeHandler(DataListener handler) {
		handlers.remove(handler);
	}

	public long getSequence() {
		return sequence;
	}
}
