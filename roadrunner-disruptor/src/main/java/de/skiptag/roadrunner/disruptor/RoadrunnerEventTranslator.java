package de.skiptag.roadrunner.disruptor;

import org.json.JSONException;

import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventTranslator;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

class RoadrunnerEventTranslator implements EventTranslator<RoadrunnerEvent> {
    private RoadrunnerEvent roadrunnerEvent;

    RoadrunnerEventTranslator(RoadrunnerEvent roadrunnerEvent) {
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