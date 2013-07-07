package de.skiptag.roadrunner.disruptor.processor.storage.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

import de.skiptag.roadrunner.dataService.DataService;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class SetAction {

    private DataService dataService;

    public SetAction(DataService dataService) {
	this.dataService = dataService;
    }

    public void handle(RoadrunnerEvent message) throws JSONException {
	String path = message.extractNodePath();
	JSONObject payload;
	if (message.has("payload")) {
	    Object obj = message.get("payload");
	    if (obj instanceof JSONObject) {
		payload = (JSONObject) obj;
		if (payload instanceof JSONObject) {
		    if (Strings.isNullOrEmpty(path)) {
			dataService.update(null, payload);
		    } else {
			dataService.update(path, payload);
		    }
		}
	    } else if (obj == null || obj == JSONObject.NULL) {
		if (!Strings.isNullOrEmpty(path)) {
		    message.put("oldValue", dataService.get(path));
		    dataService.remove(path);
		}
	    } else {
		if (!Strings.isNullOrEmpty(path)) {
		    dataService.update(path, obj);
		}
	    }
	} else {
	    if (!Strings.isNullOrEmpty(path)) {
		message.put("oldValue", dataService.get(path));
		dataService.remove(path);
	    }
	}

    }

}
