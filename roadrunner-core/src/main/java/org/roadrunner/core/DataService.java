package org.roadrunner.core;

import org.json.JSONObject;
import org.roadrunner.core.dtos.InitMessage;
import org.roadrunner.core.dtos.PushedMessage;

public interface DataService {

	PushedMessage update(String nodeName, JSONObject payload);

	void shutdown();

	InitMessage init(String path);

	void remove(String path);

	void sync(String path);

	public void updateSimpleValue(String path, Object obj);
	
	void setListener(DataListener dataListener);
}
