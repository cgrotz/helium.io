package de.skiptag.roadrunner.authorization.rulebased;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.Node;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.RoadrunnerNotAuthorizedException;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class RuleBasedAuthorization implements Authorization {

    public class AuthenticationWrapper {
	public AuthenticationWrapper(Node auth) {
	    if (auth != null && auth.has("id")) {
		this.id = auth.getString("id");
	    }
	}

	public String id;
    }

    private RuleBasedAuthorizator rule;
    ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("JavaScript");

    public RuleBasedAuthorization(Node rule) {
	this.rule = new RuleBasedAuthorizator(rule);
    }

    @Override
    public void authorize(RoadrunnerOperation op, Node auth,
	    RulesDataSnapshot root, String path, Object data)
	    throws RoadrunnerNotAuthorizedException {
	if (!isAuthorized(op, auth, root, path, data)) {
	    throw new RoadrunnerNotAuthorizedException(op, path);
	}
    }

    @Override
    public boolean isAuthorized(RoadrunnerOperation op, Node auth,
	    RulesDataSnapshot root, String path, Object data) {
	String expression = rule.getExpressionForPathAndOperation(path, op);

	try {
	    engine.put(RoadrunnerEvent.AUTH, new AuthenticationWrapper(auth));
	    Boolean result = (Boolean) engine.eval(expression);
	    return result.booleanValue();
	} catch (ScriptException ex) {
	    throw new RuntimeException(ex);
	}
    }

}