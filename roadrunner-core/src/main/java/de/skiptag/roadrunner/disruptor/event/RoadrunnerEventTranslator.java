package de.skiptag.roadrunner.disruptor.event;

import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventTranslator;

public class RoadrunnerEventTranslator implements
	EventTranslator<RoadrunnerEvent> {
    private RoadrunnerEvent roadrunnerEvent;

    public RoadrunnerEventTranslator(RoadrunnerEvent roadrunnerEvent) {
	this.roadrunnerEvent = roadrunnerEvent;
    }

    @Override
    public void translateTo(RoadrunnerEvent event, long sequence) {
	Preconditions.checkNotNull(roadrunnerEvent.getBasePath());
	event.setBasePath(roadrunnerEvent.getBasePath());
	event.populate(roadrunnerEvent.toString());
    }
}