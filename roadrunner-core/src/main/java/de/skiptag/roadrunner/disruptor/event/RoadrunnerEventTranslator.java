package de.skiptag.roadrunner.disruptor.event;

import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventTranslator;

public class RoadrunnerEventTranslator implements
	EventTranslator<RoadrunnerEvent> {
    private RoadrunnerEvent roadrunnerEvent;

    public RoadrunnerEventTranslator(RoadrunnerEvent roadrunnerEvent) {
	Preconditions.checkNotNull(roadrunnerEvent.getBasePath());
	this.roadrunnerEvent = roadrunnerEvent;
    }

    @Override
    public void translateTo(RoadrunnerEvent event, long sequence) {
	event.populate(roadrunnerEvent.toString());
    }
}