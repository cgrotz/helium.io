package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import org.json.JSONObject;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class SetAction {

    private Persistence persistence;

    public SetAction(Persistence persistence) {
	this.persistence = persistence;
    }

    public void handle(RoadrunnerEvent message) {
	Path path = new Path(message.extractNodePath());
	JSONObject payload;
	if (message.has(RoadrunnerEvent.PAYLOAD)) {
	    Object obj = message.get(RoadrunnerEvent.PAYLOAD);
	    if (obj == JSONObject.NULL || obj == null) {
		message.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
		persistence.remove(path);
	    } else if (obj instanceof JSONObject) {
		payload = (JSONObject) obj;
		if (payload instanceof JSONObject) {
		    boolean created = persistence.applyNewValue(path, payload);
		    message.created(created);
		}
	    } else if (obj == null || obj == JSONObject.NULL) {
		message.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
		persistence.remove(path);
	    } else {
		persistence.applyNewValue(path, obj);
	    }
	} else {
	    message.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
	    persistence.remove(path);
	}

    }

}
