package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class SetAction {

    private Persistence persistence;

    public SetAction(Persistence persistence) {
	this.persistence = persistence;
    }

    public void handle(RoadrunnerEvent message) throws JSONException {
	Path path = new Path(message.extractNodePath());
	JSONObject payload;
	if (message.has("payload")) {
	    Object obj = message.get("payload");
	    if (obj instanceof JSONObject) {
		payload = (JSONObject) obj;
		if (payload instanceof JSONObject) {
		    boolean created = persistence.update(path, payload);
		    message.created(created);
		}
	    } else if (obj == null || obj == JSONObject.NULL) {
		message.put("oldValue", persistence.get(path));
		persistence.remove(path);
	    } else {
		persistence.update(path, obj);
	    }
	} else {
	    message.put("oldValue", persistence.get(path));
	    persistence.remove(path);
	}

    }

}
