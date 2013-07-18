package de.skiptag.roadrunner.disruptor.processor.persistence;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.processor.persistence.actions.PushAction;
import de.skiptag.roadrunner.disruptor.processor.persistence.actions.SetAction;
import de.skiptag.roadrunner.persistence.Persistence;

public class PersistenceProcessor implements EventHandler<RoadrunnerEvent> {

    private PushAction pushAction;

    private SetAction setAction;

    public PersistenceProcessor(Persistence persistence) {
	pushAction = new PushAction(persistence);
	setAction = new SetAction(persistence);
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch) {
	switch (event.getType()) {
	case PUSH:
	    pushAction.handle(event);
	    break;
	case SET:
	    setAction.handle(event);
	    break;
	default:
	    break;
	}
    }
}
