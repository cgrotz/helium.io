package de.skiptag.roadrunner.persistence;

import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;
import de.skiptag.roadrunner.common.Path;
import de.skiptag.roadrunner.event.changelog.ChangeLog;
import de.skiptag.roadrunner.json.Node;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.queries.QueryEvaluator;

public interface Persistence extends SnapshotProcessor {

	Object get(Path path);

	Node getNode(Path path);

	void remove(ChangeLog log, Node auth, Path path);

	void applyNewValue(ChangeLog log, Node auth, Path path, int priority, Object payload);

	void updateValue(ChangeLog log, Node auth, Path path, int priority, Object payload);

	void setPriority(ChangeLog log, Node auth, Path path, int priority);

	void syncPath(Path path, RoadrunnerEndpoint handler);

	void syncPropertyValue(Path path, RoadrunnerEndpoint roadrunnerEventHandler);

	RulesDataSnapshot getRoot();

	public void syncPathWithQuery(Path path, RoadrunnerEndpoint handler,
			QueryEvaluator queryEvaluator, String query);

}
