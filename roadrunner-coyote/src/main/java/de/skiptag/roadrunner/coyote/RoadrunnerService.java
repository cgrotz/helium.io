package de.skiptag.roadrunner.coyote;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeObject;

import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;

public class RoadrunnerService extends
		de.skiptag.roadrunner.core.RoadrunnerService {

	public RoadrunnerService(AuthorizationService authorizationService,
			DataService dataService, String contextName, String path) {
		super(authorizationService, dataService, contextName, path);
	}

	public RoadrunnerService push(org.mozilla.javascript.NativeObject data)
			throws JSONException {
		return (RoadrunnerService) super.push(toJSONObject(data));
	}

	public RoadrunnerService update(org.mozilla.javascript.NativeObject data)
			throws JSONException {
		return (RoadrunnerService) super.update(toJSONObject(data));
	}

	public RoadrunnerService set(org.mozilla.javascript.NativeObject data)
			throws JSONException {
		return (RoadrunnerService) super.set(toJSONObject(data));
	}

	private JSONObject toJSONObject(NativeObject data) throws JSONException {
		JSONObject result = new JSONObject();
		for (Object keyObj : data.keySet()) {
			String key = (String) keyObj;
			Object value = data.get(key);
			if (value instanceof NativeObject) {
				result.put(key, toJSONObject((NativeObject) value));
			} else {
				result.put(key, value);
			}
		}
		return result;
	}
}
