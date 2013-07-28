package de.skiptag.roadrunner.messaging;

import org.json.Node;

public interface DataListener {

    void child_moved(Node childSnapshot, boolean hasChildren, long numChildren);

    void child_added(String name, String String, String parent, Object payload,
	    boolean hasChildren, long numChildren, int priority);

    void child_removed(String path, String name, Object payload);

    void child_changed(String name, String String, String parent,
	    Object payload, boolean hasChildren, long numChildren, int priority);

    void value(String name, String String, String parent, Object payload,
	    boolean hasChildren, long numChildren, int priority);

    void distributeEvent(String path, Node jsonObject);
}
