package io.helium.authorization.rulebased;

import io.helium.authorization.Authorization;
import io.helium.authorization.HeliumNotAuthorizedException;
import io.helium.authorization.HeliumOperation;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.json.Node;
import io.helium.scripting.SandboxedScriptingEnvironment;

public class RuleBasedAuthorization implements Authorization {

	private RuleBasedAuthorizator					rule;
	private SandboxedScriptingEnvironment	scriptingEnvironment;

	public RuleBasedAuthorization(Node rule) {
		if (rule != null && rule.has("rules")) {
			this.rule = new RuleBasedAuthorizator(rule.getNode("rules"));
		} else {
			this.rule = new RuleBasedAuthorizator(Authorization.ALL_ACCESS_RULE.getNode("rules"));
		}

		this.scriptingEnvironment = new SandboxedScriptingEnvironment();
	}

	@Override
	public void authorize(HeliumOperation op, Node auth, RulesDataSnapshot root, Path path,
			Object data) throws HeliumNotAuthorizedException {
		if (!isAuthorized(op, auth, root, path, data)) {
			throw new HeliumNotAuthorizedException(op, path);
		}
	}

	@Override
	public boolean isAuthorized(HeliumOperation op, Node auth, RulesDataSnapshot root, Path path,
			Object data) {
		String expression = rule.getExpressionForPathAndOperation(path, op);
		try {
			return Boolean.parseBoolean(expression);
		} catch (Exception e) {
			scriptingEnvironment.put(HeliumEvent.AUTH, scriptingEnvironment.eval(auth.toString()));
			Boolean result = (Boolean) scriptingEnvironment.eval(expression);
			return result.booleanValue();
		}
	}
}