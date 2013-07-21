package de.skiptag.roadrunner.disruptor.processor.authorization;

import org.json.JSONObject;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryDataSnapshot;

public class AuthorizationProcessor implements EventHandler<RoadrunnerEvent> {

    private Authorization authorization;

    private Persistence persistence;

    public AuthorizationProcessor(Authorization authorization,
	    Persistence persistence) {
	this.authorization = authorization;
	this.persistence = persistence;
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch) {
	Path path = new Path(event.extractNodePath());
	if (event.getType() == RoadrunnerEventType.PUSH) {
	    InMemoryDataSnapshot root = new InMemoryDataSnapshot(
		    persistence.get(null));
	    InMemoryDataSnapshot data = new InMemoryDataSnapshot(
		    event.has(RoadrunnerEvent.PAYLOAD) ? event.get(RoadrunnerEvent.PAYLOAD)
			    : null);
	    authorization.authorize(RoadrunnerOperation.WRITE, getAuth(event), root, path.toString(), data);
	} else if (event.getType() == RoadrunnerEventType.SET) {
	    if (event.has(RoadrunnerEvent.PAYLOAD)
		    && event.get(RoadrunnerEvent.PAYLOAD) == JSONObject.NULL) {
		InMemoryDataSnapshot root = new InMemoryDataSnapshot(
			persistence.get(null));
		InMemoryDataSnapshot data = new InMemoryDataSnapshot(
			event.get(RoadrunnerEvent.PAYLOAD));
		authorization.authorize(RoadrunnerOperation.WRITE, getAuth(event), root, path.toString(), data);
	    } else {
		InMemoryDataSnapshot root = new InMemoryDataSnapshot(
			persistence.get(null));
		authorization.authorize(RoadrunnerOperation.REMOVE, getAuth(event), root, path.toString(), null);
	    }
	}
    }

    private JSONObject getAuth(RoadrunnerEvent event) {
	if (event.has(RoadrunnerEvent.AUTH)) {
	    return event.getJSONObject(RoadrunnerEvent.AUTH);
	} else {
	    return new JSONObject();
	}
    }
}
