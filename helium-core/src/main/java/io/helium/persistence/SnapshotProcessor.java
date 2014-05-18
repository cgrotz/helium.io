package io.helium.persistence;

import io.helium.json.Node;

public interface SnapshotProcessor {

    Node dumpSnapshot();

    void restoreSnapshot(Node node);

}