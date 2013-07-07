package de.skiptag.roadrunner.persistence;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;

public interface Persistence {

    public interface QueryCallback {
	public void change(String path, JSONObject value, String parentPath,
		long numChildren, String name, boolean hasChildren, int priority);
    }

    Object get(String path);

    String getName(String path);

    String getParent(String path);

    void query(String expression, QueryCallback queryCallback);

    void remove(String path);

    void shutdown();

    void sync(String path, RoadrunnerEventHandler handler);

    void update(String nodeName, Object payload);

    void setAuth(JSONObject auth);

    JSONObject dumpSnapshot();

    void restoreSnapshot(JSONObject payload) throws JSONException;

}
