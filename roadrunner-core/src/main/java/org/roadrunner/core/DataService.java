package org.roadrunner.core;

import org.json.JSONObject;
import org.roadrunner.core.dtos.PushedMessage;

public interface DataService {

	void addListener(DataListener dataListener);

	JSONObject get(String path);

	String getName(String path);

	String getParent(String path);

	void remove(String path);

	void removeListener(DataListener dataListener);

	void shutdown();

	void sync(String path);

	PushedMessage update(String nodeName, JSONObject payload);

	void updateSimpleValue(String path, Object obj);
}
