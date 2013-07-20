package de.skiptag.roadrunner.persistence;

import org.json.JSONObject;

import de.skiptag.roadrunner.persistence.inmemory.Node;

public interface SnapshotProcessor {

    JSONObject dumpSnapshot();

    void restoreSnapshot(Node node);

}