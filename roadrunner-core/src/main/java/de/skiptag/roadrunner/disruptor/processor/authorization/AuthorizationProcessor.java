package de.skiptag.roadrunner.disruptor.processor.authorization;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class AuthorizationProcessor implements EventHandler<RoadrunnerEvent> {

    private AuthorizationService service;

    public AuthorizationProcessor(AuthorizationService service) {
	this.service = service;
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
