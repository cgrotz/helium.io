package org.roadrunner.core;

import org.json.JSONObject;

public interface DataListener {

	void child_moved(JSONObject childSnapshot, String prevChildName);

	void child_added(String name, String path, String parent, JSONObject node, String prevChildName);

	void child_removed(JSONObject childSnapshot);

}
