package org.roadrunner.core;

import org.json.JSONObject;

public interface DataService {

	void setListener(DataListener dataListener);

	void update(String nodeName, JSONObject payload);

	void shutdown();

	void sync();
}
