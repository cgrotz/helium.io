package de.skiptag.roadrunner.core.authorization;

import org.json.JSONObject;

import de.skiptag.roadrunner.core.authorization.impl.RoadrunnerNotAuthorizedException;
import de.skiptag.roadrunner.core.authorization.rulebased.RulesDataSnapshot;

public interface AuthorizationService {

    void shutdown();

    void authorize(RoadrunnerOperation remove, JSONObject auth,
	    RulesDataSnapshot root, String path, RulesDataSnapshot objectForPath)
	    throws RoadrunnerNotAuthorizedException;

    boolean isAuthorized(RoadrunnerOperation read, JSONObject auth,
	    RulesDataSnapshot root, String path, RulesDataSnapshot objectForPath);
}
