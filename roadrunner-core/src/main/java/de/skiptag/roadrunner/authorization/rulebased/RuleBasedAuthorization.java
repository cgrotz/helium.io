package de.skiptag.roadrunner.authorization.rulebased;

import org.json.Node;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.RoadrunnerNotAuthorizedException;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.scripting.SandboxedScriptingEnvironment;

public class RuleBasedAuthorization implements Authorization {

	private RuleBasedAuthorizator rule;
	private SandboxedScriptingEnvironment scriptingEnvironment;

	public RuleBasedAuthorization(Node rule) {
		this.rule = new RuleBasedAuthorizator(rule.has("rules") ? rule.getNode("rules")
				: Authorization.ALL_ACCESS_RULE.getNode("rules"));
		this.scriptingEnvironment = new SandboxedScriptingEnvironment();
	}

	@Override
	public void authorize(RoadrunnerOperation op, Node auth, RulesDataSnapshot root, Path path,
			Object data) throws RoadrunnerNotAuthorizedException {
		if (!isAuthorized(op, auth, root, path, data)) {
			throw new RoadrunnerNotAuthorizedException(op, path);
		}
	}

	@Override
	public boolean isAuthorized(RoadrunnerOperation op, Node auth, RulesDataSnapshot root,
			Path path, Object data) {
		String expression = rule.getExpressionForPathAndOperation(path, op);
		try {
			return Boolean.parseBoolean(expression);
		} catch (Exception e) {
			scriptingEnvironment.put(RoadrunnerEvent.AUTH,
					scriptingEnvironment.eval(auth.toString()));
			Boolean result = (Boolean) scriptingEnvironment.eval(expression);
			return result.booleanValue();
		}
	}
}