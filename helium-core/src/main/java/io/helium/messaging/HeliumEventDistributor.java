package io.helium.messaging;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.json.Node;

public interface HeliumEventDistributor {
	void distribute(HeliumEvent event);

	void distributeEvent(Path path, Node payload);
}
