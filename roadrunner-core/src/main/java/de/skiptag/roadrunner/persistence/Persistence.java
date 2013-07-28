package de.skiptag.roadrunner.persistence;

import org.json.Node;

import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;

public interface Persistence extends SnapshotProcessor {

    Object get(Path path);

    Node getNode(Path path);

    void remove(Path path);

    boolean applyNewValue(Path path, int priority, Object payload);

    void setPriority(Path path, int priority);

    void syncPath(Path path, RoadrunnerEndpoint handler);

    void syncPropertyValue(Path path, RoadrunnerEndpoint roadrunnerEventHandler);

}
