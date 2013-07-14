package de.skiptag.roadrunner.disruptor.processor.authorization;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Persistence;

public class AuthorizationProcessor implements EventHandler<RoadrunnerEvent> {

    private Authorization service;

    private Persistence persistence;

    public AuthorizationProcessor(Authorization service, Persistence persistence) {
	this.service = service;
	this.persistence = persistence;
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch)
	    throws Exception {

	// if (!service.authorize(RoadrunnerOperation, auth, root, path, data))
	// {
	// throw new OperationUnauthorizedException();
	// }
    }
}
