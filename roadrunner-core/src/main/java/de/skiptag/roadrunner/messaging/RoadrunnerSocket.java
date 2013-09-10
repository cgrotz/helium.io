package de.skiptag.roadrunner.messaging;

import de.skiptag.roadrunner.common.Path;
import de.skiptag.roadrunner.event.RoadrunnerEvent;
import de.skiptag.roadrunner.json.Node;

public interface RoadrunnerSocket {
	void send(String string);

	void distribute(RoadrunnerEvent event);

	void distributeEvent(Path path, Node payload);
}
