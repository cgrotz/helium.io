package de.skiptag.roadrunner.coyote.actions;

import org.json.JSONObject;

import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.Persistence.QueryCallback;
import de.skiptag.roadrunner.coyote.RoadrunnerModule;

public class QueryAction implements QueryCallback {

    private RoadrunnerModule roadrunnerModule;
    private Persistence persistence;

    public QueryAction(Persistence persistence,
	    RoadrunnerModule roadrunnerModule) {
	this.roadrunnerModule = roadrunnerModule;
	this.persistence = persistence;
    }

    @Override
    public void change(String nodepath, JSONObject value, String parentPath,
	    long numChildren, String name, boolean hasChildren, int priority) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("name", name);
	    broadcast.put("parent", parentPath);
	    broadcast.put("payload", value.toString());
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    roadrunnerModule.send(broadcast.toString());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void handle(String query) {
	persistence.query(query, this);
    }
}
