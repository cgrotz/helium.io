package org.roadrunner.core;

import org.json.JSONObject;

public interface DataListener {

	void child_moved(JSONObject childSnapshot, String prevChildName, boolean hasChildren, long numChildren);

	void child_added(String name, String path, String parent, JSONObject node, String prevChildName, boolean hasChildren, long numChildren);

	void child_removed(JSONObject childSnapshot);

	public void child_changed(String name, String path, String parent, JSONObject node, String prevChildName, boolean hasChildren, long numChildren);
}
