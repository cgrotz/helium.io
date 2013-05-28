package de.skiptag.roadrunner.core;

import org.json.JSONObject;

import de.skiptag.roadrunner.core.dtos.PushedMessage;

public interface DataService {

	public interface QueryCallback {
		public void change(String path, JSONObject value, String parentPath,
				long numChildren, String name, boolean hasChildren, int priority);
	}

	void addListener(DataListener dataListener);

	JSONObject get(String path);

	String getName(String path);

	String getParent(String path);

	void query(String expression, QueryCallback queryCallback);

	void remove(String path);

	void removeListener(DataListener dataListener);

	void shutdown();

	void sync(String path);

	PushedMessage update(String nodeName, JSONObject payload);

	void updateSimpleValue(String path, Object obj);
}
