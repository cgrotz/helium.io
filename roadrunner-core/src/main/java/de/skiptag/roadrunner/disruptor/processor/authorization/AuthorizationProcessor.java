package de.skiptag.roadrunner.disruptor.processor.authorization;

import org.json.JSONObject;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.DataSnapshot;

public class AuthorizationProcessor implements EventHandler<RoadrunnerEvent> {

    private Authorization authorization;

    private Persistence persistence;

    private JSONObject auth;

    public AuthorizationProcessor(Authorization authorization,
	    Persistence persistence) {
	this.authorization = authorization;
	this.persistence = persistence;
	this.auth = new JSONObject();
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch)
	    throws Exception {
	Path path = new Path(event.extractNodePath());
	if (event.getType() == RoadrunnerEventType.PUSH) {
	    DataSnapshot root = new DataSnapshot(persistence.get(null));
	    DataSnapshot data = new DataSnapshot(event.get("payload"));
	    authorization.authorize(RoadrunnerOperation.WRITE, auth, root, path.toString(), data);
	} else if (event.getType() == RoadrunnerEventType.SET) {
	    if (event.has("payload")) {
		DataSnapshot root = new DataSnapshot(persistence.get(null));
		DataSnapshot data = new DataSnapshot(event.get("payload"));
		authorization.authorize(RoadrunnerOperation.WRITE, auth, root, path.toString(), data);
	    } else {
		DataSnapshot root = new DataSnapshot(persistence.get(null));
		authorization.authorize(RoadrunnerOperation.REMOVE, auth, root, path.toString(), null);
	    }
	}
    }
}
