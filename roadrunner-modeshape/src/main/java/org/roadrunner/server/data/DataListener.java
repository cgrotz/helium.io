package org.roadrunner.server.data;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.json.JSONException;
import org.json.JSONObject;

public interface DataListener {

	void child_moved(JSONObject childSnapshot, String prevChildName) throws JSONException, IOException;

	void child_added(String name, String path, String parent, JSONObject node, String prevChildName) throws JSONException, IOException, RepositoryException;

	void child_removed(JSONObject childSnapshot) throws JSONException, IOException;

}
