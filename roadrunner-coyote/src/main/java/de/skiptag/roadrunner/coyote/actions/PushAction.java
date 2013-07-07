package de.skiptag.roadrunner.coyote.actions;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

import de.skiptag.roadrunner.dataService.DataService;

public class PushAction {

    private DataService dataService;

    public PushAction(DataService dataService) {
	this.dataService = dataService;
    }

    public void handle(JSONObject message, String path) throws JSONException {
	JSONObject payload;
	if (message.has("payload")) {
	    payload = (JSONObject) message.get("payload");
	} else {
	    payload = new JSONObject();
	}
	String nodeName;
	if (message.has("name")) {
	    nodeName = message.getString("name");
	} else {
	    nodeName = UUID.randomUUID().toString().replaceAll("-", "");
	}
	if (Strings.isNullOrEmpty(path)) {
	    dataService.update(nodeName, payload);
	} else {
	    dataService.update(path + "/" + nodeName, payload);
	}
    }

}
