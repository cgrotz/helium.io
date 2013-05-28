package de.skiptag.roadrunner.core.authorization;

import org.json.JSONObject;

public interface AuthorizationService {

	void shutdown();

	boolean authorize(RoadrunnerOperation operation, JSONObject auth,
			RulesDataSnapshot root, String path, RulesDataSnapshot newData);
}
