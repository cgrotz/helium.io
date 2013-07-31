package de.skiptag.roadrunner.disruptor.event;

import com.lmax.disruptor.EventTranslator;

public class RoadrunnerEventTranslator implements
	EventTranslator<RoadrunnerEvent> {
    private RoadrunnerEvent roadrunnerEvent;
    private long sequence;

    public long getSequence() {
	return sequence;
    }

    public RoadrunnerEventTranslator(RoadrunnerEvent roadrunnerEvent) {
	this.roadrunnerEvent = roadrunnerEvent;
    }

    @Override
    public void translateTo(RoadrunnerEvent event, long sequence) {
	event.populate(roadrunnerEvent.toString());
	this.sequence = sequence;
    }
}