package de.skiptag.roadrunner.coyote.actions;

import org.json.JSONObject;

import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.DataService.QueryCallback;
import de.skiptag.roadrunner.coyote.RoadrunnerModule;

public class QueryAction implements QueryCallback {

	private RoadrunnerModule roadrunnerModule;
	private DataService dataService;

	public QueryAction(DataService dataService,
			RoadrunnerModule roadrunnerModule) {
		this.roadrunnerModule = roadrunnerModule;
		this.dataService = dataService;
	}

	@Override
	public void change(String nodepath, JSONObject value, String parentPath,
			long numChildren, String name, boolean hasChildren, int priority) {
		try {
			JSONObject broadcast = new JSONObject();
			// broadcast.put("type", queryName);
			broadcast.put("name", name);
			// broadcast.put("path", path + "/" + nodepath);
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
		dataService.query(query, this);
	}
}
