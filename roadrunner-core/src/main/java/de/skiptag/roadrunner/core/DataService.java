package de.skiptag.roadrunner.core;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.core.dtos.PushedMessage;

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

    PushedMessage update(String nodeName, Object payload);

    void updateSimpleValue(String path, Object obj);

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
