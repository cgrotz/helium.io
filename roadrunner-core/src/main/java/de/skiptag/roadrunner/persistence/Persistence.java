package de.skiptag.roadrunner.persistence;

import org.json.Node;

import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;
import de.skiptag.roadrunner.disruptor.event.changelog.ChangeLog;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;

public interface Persistence extends SnapshotProcessor {

    Object get(Path path);

    Node getNode(Path path);

    void remove(ChangeLog log, Path path);

    void applyNewValue(ChangeLog log, Path path, int priority, Object payload);

    void setPriority(ChangeLog log, Path path, int priority);

    void syncPath(Path path, RoadrunnerEndpoint handler);

    void syncPropertyValue(Path path, RoadrunnerEndpoint roadrunnerEventHandler);

    RulesDataSnapshot getRoot();

}
