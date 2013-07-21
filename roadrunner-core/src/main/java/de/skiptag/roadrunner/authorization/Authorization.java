package de.skiptag.roadrunner.authorization;

import org.json.JSONObject;

import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;

public interface Authorization {

    void authorize(RoadrunnerOperation op, JSONObject auth,
	    RulesDataSnapshot root, String path, Object object)
	    throws RoadrunnerNotAuthorizedException;

    boolean isAuthorized(RoadrunnerOperation read, JSONObject auth,
	    RulesDataSnapshot root, String path, Object object);

}
