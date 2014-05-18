package io.helium.disruptor.translator;

import com.lmax.disruptor.EventTranslator;

import io.helium.event.HeliumEvent;

public class HeliumEventTranslator implements EventTranslator<HeliumEvent> {
	private HeliumEvent	heliumEvent;
	private long						sequence;

	public long getSequence() {
		return sequence;
	}

	public HeliumEventTranslator(HeliumEvent heliumEvent) {
		this.heliumEvent = heliumEvent;
	}

	@Override
	public void translateTo(HeliumEvent event, long sequence) {
		event.clear();
		event.populate(heliumEvent.toString());
		this.sequence = sequence;
	}
}