package io.helium.persistence;

import io.helium.authorization.rulebased.RulesDataSnapshot;
import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.messaging.HeliumEndpoint;
import io.helium.queries.QueryEvaluator;

public interface Persistence extends SnapshotProcessor {

	Object get(Path path);

	Node getNode(Path path);

	void remove(ChangeLog log, Node auth, Path path);

	void applyNewValue(ChangeLog log, Node auth, Path path, int priority, Object payload);

	void updateValue(ChangeLog log, Node auth, Path path, int priority, Object payload);

	void setPriority(ChangeLog log, Node auth, Path path, int priority);

	void syncPath(Path path, HeliumEndpoint handler);

	void syncPropertyValue(Path path, HeliumEndpoint heliumEventHandler);

	RulesDataSnapshot getRoot();

	public void syncPathWithQuery(Path path, HeliumEndpoint handler,
			QueryEvaluator queryEvaluator, String query);

}
