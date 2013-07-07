package de.skiptag.roadrunner.authorization;

import org.json.JSONObject;

import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;

public interface Authorization {

    void shutdown();

    void authorize(RoadrunnerOperation remove, JSONObject auth,
	    RulesDataSnapshot root, String path, RulesDataSnapshot objectForPath)
	    throws RoadrunnerNotAuthorizedException;

    boolean isAuthorized(RoadrunnerOperation read, JSONObject auth,
	    RulesDataSnapshot root, String path, RulesDataSnapshot objectForPath);
}
