package de.skiptag.roadrunner.persistence;

import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;

public interface Persistence extends SnapshotProcessor {

    Object get(Path path);

    void remove(Path path);

    boolean applyNewValue(Path path, Object payload);

    void syncPath(Path path, RoadrunnerEventHandler handler);

}
