package de.skiptag.roadrunner.coyote.actions;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

import de.skiptag.roadrunner.persistence.Persistence;

public class PushAction {

    private Persistence persistence;

    public PushAction(Persistence persistence) {
	this.persistence = persistence;
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
	    persistence.update(nodeName, payload);
	} else {
	    persistence.update(path + "/" + nodeName, payload);
	}
    }

}
