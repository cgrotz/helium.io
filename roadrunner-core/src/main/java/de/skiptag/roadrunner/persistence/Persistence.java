package de.skiptag.roadrunner.persistence;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;

public interface Persistence {

    Object get(Path path);

    String getName(Path path);

    String getParent(Path path);

    void remove(Path path);

    void shutdown();

    void sync(Path path, RoadrunnerEventHandler handler);

    boolean update(Path path, Object payload);

    JSONObject dumpSnapshot();

    void restoreSnapshot(JSONObject payload) throws JSONException;

}
