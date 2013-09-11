package de.skiptag.roadrunner.disruptor.translator;

import com.lmax.disruptor.EventTranslator;

import de.skiptag.roadrunner.event.RoadrunnerEvent;

public class RoadrunnerEventTranslator implements EventTranslator<RoadrunnerEvent> {
	private RoadrunnerEvent	roadrunnerEvent;
	private long						sequence;

	public long getSequence() {
		return sequence;
	}

	public RoadrunnerEventTranslator(RoadrunnerEvent roadrunnerEvent) {
		this.roadrunnerEvent = roadrunnerEvent;
	}

	@Override
	public void translateTo(RoadrunnerEvent event, long sequence) {
		event.clear();
		event.populate(roadrunnerEvent.toString());
		this.sequence = sequence;
	}
}