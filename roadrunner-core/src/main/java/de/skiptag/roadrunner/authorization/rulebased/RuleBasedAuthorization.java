package de.skiptag.roadrunner.authorization.rulebased;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.JSONObject;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.RoadrunnerNotAuthorizedException;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;

public class RuleBasedAuthorization implements Authorization {

    public class AuthenticationWrapper {
	public AuthenticationWrapper(JSONObject auth) {
	    if (auth != null && auth.has("id")) {
		this.id = auth.getString("id");
	    }
	}

	public String id;
    }

    private RuleBasedAuthorizator rule;
    ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("JavaScript");

    public RuleBasedAuthorization(JSONObject rule) {
	this.rule = new RuleBasedAuthorizator(rule);
    }

    @Override
    public void authorize(RoadrunnerOperation op, JSONObject auth,
	    RulesDataSnapshot root, String path, Object data)
	    throws RoadrunnerNotAuthorizedException {
	if (!isAuthorized(op, auth, root, path, data)) {
	    throw new RoadrunnerNotAuthorizedException(op, path);
	}
    }

    @Override
    public boolean isAuthorized(RoadrunnerOperation op, JSONObject auth,
	    RulesDataSnapshot root, String path, Object data) {
	String expression = rule.getExpressionForPathAndOperation(path, op);

	try {
	    engine.put("auth", new AuthenticationWrapper(auth));
	    Boolean result = (Boolean) engine.eval(expression);
	    return result.booleanValue();
	} catch (ScriptException ex) {
	    throw new RuntimeException(ex);
	}
    }

}