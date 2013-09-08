package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import java.util.UUID;

import de.skiptag.roadrunner.common.Path;
import de.skiptag.roadrunner.event.RoadrunnerEvent;
import de.skiptag.roadrunner.json.Node;
import de.skiptag.roadrunner.persistence.Persistence;

public class PushAction {

	private Persistence persistence;

	public PushAction(Persistence persistence) {
		this.persistence = persistence;
	}

	public void handle(RoadrunnerEvent event) {
		Path path = event.extractNodePath();
		Object payload;
		if (event.has(RoadrunnerEvent.PAYLOAD)) {
			payload = event.get(RoadrunnerEvent.PAYLOAD);
		} else {
			payload = new Node();
		}

		String nodeName;
		if (event.has("name")) {
			nodeName = event.getString("name");
		} else {
			nodeName = UUID.randomUUID().toString().replaceAll("-", "");
		}
		if (path.isEmtpy()) {
			persistence.applyNewValue(event.getChangeLog(), event.getAuth(), new Path(nodeName),
					-1, payload);
		} else {
			persistence.applyNewValue(event.getChangeLog(), event.getAuth(), path, -1, payload);
		}
	}

}
