package de.skiptag.roadrunner.disruptor.event;

import org.json.JSONException;

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
	try {
	    Preconditions.checkNotNull(roadrunnerEvent.getBasePath());
	    Preconditions.checkNotNull(roadrunnerEvent.getRepositoryName());
	    event.setBasePath(roadrunnerEvent.getBasePath());
	    event.setRepositoryName(roadrunnerEvent.getRepositoryName());
	    event.populate(roadrunnerEvent.toString());
	} catch (JSONException e) {
	    e.printStackTrace();
	}
    }
}