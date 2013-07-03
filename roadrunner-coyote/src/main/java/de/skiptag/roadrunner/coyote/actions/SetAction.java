package de.skiptag.roadrunner.coyote.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

import de.skiptag.roadrunner.core.DataService;

public class SetAction {

	private DataService dataService;

	public SetAction(DataService dataService) {
		this.dataService = dataService;
	}

	public void handle(JSONObject message, String path) throws JSONException {
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
					dataService.updateSimpleValue(path, obj);
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
