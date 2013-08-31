package de.skiptag.roadrunner.persistence;

import de.skiptag.roadrunner.json.Node;

public interface SnapshotProcessor {

    Node dumpSnapshot();

    void restoreSnapshot(Node node);

}