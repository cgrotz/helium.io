package de.skiptag.roadrunner.persistence;

import org.json.Node;

public interface SnapshotProcessor {

    Node dumpSnapshot();

    void restoreSnapshot(Node node);

}