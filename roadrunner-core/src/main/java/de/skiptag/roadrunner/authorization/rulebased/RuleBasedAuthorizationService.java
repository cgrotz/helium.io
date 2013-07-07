package de.skiptag.roadrunner.authorization.rulebased;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.authorization.AuthorizationService;
import de.skiptag.roadrunner.authorization.RoadrunnerNotAuthorizedException;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;

public class RuleBasedAuthorizationService implements AuthorizationService {

	public class AuthenticationWrapper {
		public AuthenticationWrapper(JSONObject auth) throws JSONException {
			this.id = auth.getString("id");
		}

		public String id;

	}

	private RuleBasedAuthorizator rule;
	ScriptEngineManager mgr = new ScriptEngineManager();
	ScriptEngine engine = mgr.getEngineByName("JavaScript");

	public RuleBasedAuthorizationService(JSONObject rule) throws JSONException {
		this.rule = new RuleBasedAuthorizator(rule);
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void authorize(RoadrunnerOperation op, JSONObject auth,
			RulesDataSnapshot root, String path, RulesDataSnapshot data)
			throws RoadrunnerNotAuthorizedException {
		if (!isAuthorized(op, auth, root, path, data)) {
			throw new RoadrunnerNotAuthorizedException(op, path);
		}
	}

	@Override
	public boolean isAuthorized(RoadrunnerOperation op, JSONObject auth,
			RulesDataSnapshot root, String path, RulesDataSnapshot data) {
		String expression = rule.getExpressionForPathAndOperation(path, op);

		try {
			engine.put("auth", new AuthenticationWrapper(auth));
			Boolean result = (Boolean) engine.eval(expression);
			return result.booleanValue();
		} catch (ScriptException ex) {
			throw new RuntimeException(ex);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

}