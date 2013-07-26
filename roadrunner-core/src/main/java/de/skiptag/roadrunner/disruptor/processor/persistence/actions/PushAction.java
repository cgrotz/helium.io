package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import java.util.UUID;

import org.json.JSONObject;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class PushAction {

    private Persistence persistence;

    public PushAction(Persistence persistence) {
	this.persistence = persistence;
    }

    public void handle(RoadrunnerEvent message) {
	Path path = new Path(message.extractNodePath());
	Object payload;
	if (message.has(RoadrunnerEvent.PAYLOAD)) {
	    payload = message.get(RoadrunnerEvent.PAYLOAD);
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
	    persistence.applyNewValue(new Path(nodeName), payload);
	} else {
	    persistence.applyNewValue(path, payload);
	}
    }

}
