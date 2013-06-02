package de.skiptag.roadrunner.modeshape;

import javax.jcr.Session;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.core.authorization.RoadrunnerNotAuthorizedException;
import de.skiptag.roadrunner.core.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.core.authorization.RulesDataSnapshot;

public class ModeShapeAuthorizationService implements AuthorizationService {

	public class AuthenticationWrapper {
		public AuthenticationWrapper(JSONObject auth) throws JSONException {
			this.id = auth.getString("id");
		}

		public String id;

	}

	private String repositoryName;
	private Session commonRepo;
	private RuleBasedAuthorizator rule;
	ScriptEngineManager mgr = new ScriptEngineManager();
	ScriptEngine engine = mgr.getEngineByName("JavaScript");

	public ModeShapeAuthorizationService(Session commonRepo,
			String repositoryName, JSONObject rule) throws JSONException {
		this.commonRepo = commonRepo;
		this.repositoryName = repositoryName;
		this.rule = new RuleBasedAuthorizator(rule);
	}

	@Override
	public void shutdown() {
		commonRepo.logout();
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