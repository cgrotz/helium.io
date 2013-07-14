package de.skiptag.roadrunner.persistence;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;

public interface Persistence {

    public interface QueryCallback {
	public void change(Path path, JSONObject value, Path parentPath,
		long numChildren, String name, boolean hasChildren, int priority);
    }

    Object get(Path path);

    String getName(Path path);

    String getParent(Path path);

    void query(String expression, QueryCallback queryCallback);

    void remove(Path path);

    void shutdown();

    void sync(Path path, RoadrunnerEventHandler handler);

    void update(Path path, Object payload);

    JSONObject dumpSnapshot();

    void restoreSnapshot(JSONObject payload) throws JSONException;

}
