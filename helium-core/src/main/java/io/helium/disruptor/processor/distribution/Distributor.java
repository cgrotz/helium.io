package io.helium.disruptor.processor.distribution;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.lmax.disruptor.EventHandler;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.json.Node;
import io.helium.messaging.HeliumOutboundSocket;
import io.helium.messaging.HeliumEventDistributor;

public class Distributor implements EventHandler<HeliumEvent> {

	private static final Logger		logger		= LoggerFactory.getLogger(Distributor.class);

	private Set<HeliumEventDistributor>	handlers	= Sets.newHashSet();

	private long									sequence;

	@Override
	public void onEvent(HeliumEvent event, long sequence, boolean endOfBatch) {
		this.sequence = sequence;
		distribute(event);
	}

	public void distribute(HeliumEvent event) {
		logger.trace("distributing event: " + event);
		if (!event.isFromHistory()) {
			for (HeliumEventDistributor handler : Sets.newHashSet(handlers)) {
				handler.distribute(event);
			}
		}
	}

	public void distribute(String path, Node data) {
		for (HeliumEventDistributor handler : Sets.newHashSet(handlers)) {
			handler.distributeEvent(new Path(HeliumEvent.extractPath(path)), data);
		}
	}

	public void addHandler(HeliumEventDistributor handler) {
		handlers.add(handler);
	}

	public void removeHandler(HeliumOutboundSocket handler) {
		handlers.remove(handler);
	}

	public long getSequence() {
		return sequence;
	}
}
