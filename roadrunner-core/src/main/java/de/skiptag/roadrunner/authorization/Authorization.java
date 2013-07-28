package de.skiptag.roadrunner.authorization;

import org.json.Node;

import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;

public interface Authorization {

    void authorize(RoadrunnerOperation op, Node auth,
	    RulesDataSnapshot root, String path, Object object)
	    throws RoadrunnerNotAuthorizedException;

    boolean isAuthorized(RoadrunnerOperation read, Node auth,
	    RulesDataSnapshot root, String path, Object object);

}
