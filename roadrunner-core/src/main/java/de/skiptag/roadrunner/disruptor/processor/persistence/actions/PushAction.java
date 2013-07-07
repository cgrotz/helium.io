package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class PushAction {

    private Persistence persistence;

    public PushAction(Persistence persistence) {
	this.persistence = persistence;
    }

    public void handle(RoadrunnerEvent message) throws JSONException {
	Path path = new Path(message.extractNodePath());
	Object payload;
	if (message.has("payload")) {
	    payload = message.get("payload");
	} else {
	    payload = new JSONObject();
	}

	String nodeName;
	if (message.has("name")) {
	    nodeName = message.getString("name");
	} else {
	    nodeName = UUID.randomUUID().toString().replaceAll("-", "");
	}
	if (path.isEmtpy()) {
	    persistence.update(new Path(nodeName), payload);
	} else {
	    persistence.update(path.append(nodeName), payload);
	}
    }

}
