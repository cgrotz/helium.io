package de.skiptag.roadrunner.authorization;

import org.json.Node;

import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;
import de.skiptag.roadrunner.persistence.Path;

public interface Authorization {
	public static final Node ALL_ACCESS_RULE = new Node(
			"{rules:{\".write\": \"true\",\".read\": \"true\"}}");

	void authorize(RoadrunnerOperation op, Node auth, RulesDataSnapshot root, Path path,
			Object object) throws RoadrunnerNotAuthorizedException;

	boolean isAuthorized(RoadrunnerOperation op, Node auth, RulesDataSnapshot root, Path path,
			Object object);
}
