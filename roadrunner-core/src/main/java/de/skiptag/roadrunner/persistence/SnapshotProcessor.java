package de.skiptag.roadrunner.persistence;

import org.json.JSONObject;

public interface SnapshotProcessor {

    JSONObject dumpSnapshot();

    void restoreSnapshot(JSONObject payload);

}