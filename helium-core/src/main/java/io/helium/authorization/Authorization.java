package io.helium.authorization;

import io.helium.authorization.rulebased.RulesDataSnapshot;
import io.helium.common.Path;
import io.helium.json.Node;

public interface Authorization {
	public static final Node ALL_ACCESS_RULE = new Node(
			"{rules:{\".write\": \"true\",\".read\": \"true\"}}");

	void authorize(HeliumOperation op, Node auth, RulesDataSnapshot root, Path path,
			Object object) throws HeliumNotAuthorizedException;

	boolean isAuthorized(HeliumOperation op, Node auth, RulesDataSnapshot root, Path path,
			Object object);
}
