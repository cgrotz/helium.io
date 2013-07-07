package de.skiptag.roadrunner.disruptor.processor.storage.actions;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.dtos.PushedMessage;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class PushAction {

    private DataService dataService;

    public PushAction(DataService dataService) {
	this.dataService = dataService;
    }

    public void handle(RoadrunnerEvent message) throws JSONException {
	String path = message.extractNodePath();
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
	PushedMessage pushed;
	if (Strings.isNullOrEmpty(path)) {
	    pushed = dataService.update(nodeName, payload);
	} else {
	    pushed = dataService.update(path + "/" + nodeName, payload);
	}
	{
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "pushed");
	    broadcast.put("name", nodeName);
	    broadcast.put("path", path + "/" + nodeName);
	    broadcast.put("parent", pushed.getParent());
	    broadcast.put("payload", pushed.getPayload());
	    broadcast.put("prevChildName", pushed.getPrevChildName());
	    broadcast.put("hasChildren", pushed.getHasChildren());
	    broadcast.put("numChildren", pushed.getNumChildren());
	    // send(broadcast.toString());
	}
    }

}
