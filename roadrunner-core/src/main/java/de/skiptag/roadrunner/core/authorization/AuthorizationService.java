package de.skiptag.roadrunner.core.authorization;

import org.json.JSONObject;

public interface AuthorizationService {

	void shutdown();

	void authorize(RoadrunnerOperation remove, JSONObject auth,
			RulesDataSnapshot root, String path, RulesDataSnapshot data)
			throws RoadrunnerNotAuthorizedException;

	boolean isAuthorized(RoadrunnerOperation read, JSONObject auth,
			RulesDataSnapshot modeshapeRulesDataSnapshot, String path,
			RulesDataSnapshot modeshapeRulesDataSnapshot2);
}
