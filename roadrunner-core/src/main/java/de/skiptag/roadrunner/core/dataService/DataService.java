package de.skiptag.roadrunner.core.dataService;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.core.messaging.DataListener;

public interface DataService {

    public interface QueryCallback {
	public void change(String path, JSONObject value, String parentPath,
		long numChildren, String name, boolean hasChildren, int priority);
    }

    Object get(String path);

    String getName(String path);

    String getParent(String path);

    void query(String expression, QueryCallback queryCallback);

    void remove(String path);

    void addListener(DataListener dataListener);

    void removeListener(DataListener dataListener);

    void shutdown();

    void sync(String path);

    void update(String nodeName, Object payload);

    void setAuth(JSONObject auth);

    public void fireChildRemoved(String path, JSONObject fromRemovedNodes);

    public void fireChildMoved(JSONObject childSnapshot, String prevChildName,
	    boolean hasNodes, long size);

    public void fireChildChanged(String name, String path, String parentName,
	    Object payload, String prevChildName, boolean hasNodes, long size);

    public void fireChildAdded(String name, String path, String parentName,
	    Object payload, String prevChildName, boolean hasNodes, long size);

    JSONObject dumpSnapshot();

    void restoreSnapshot(JSONObject payload) throws JSONException;

}
