package de.skiptag.roadrunner.messaging;

import org.json.JSONObject;

public interface DataListener {

    void child_moved(JSONObject childSnapshot, boolean hasChildren,
	    long numChildren);

    void child_added(String name, String String, String parent, Object payload,
	    boolean hasChildren, long numChildren);

    void child_removed(String String, Object payload);

    void child_changed(String name, String String, String parent,
	    Object payload, boolean hasChildren, long numChildren);
}
