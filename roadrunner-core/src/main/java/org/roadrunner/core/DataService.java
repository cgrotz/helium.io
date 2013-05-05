package org.roadrunner.core;

import org.json.JSONObject;
import org.roadrunner.core.dtos.PushedMessage;

public interface DataService {

	PushedMessage update(String nodeName, JSONObject payload);

	void remove(String path);

	void sync(String path);

	void updateSimpleValue(String path, Object obj);
	
	void addListener(DataListener dataListener);
	
	void removeListener(DataListener dataListener);

	void shutdown();
}
