package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import java.util.UUID;

import org.json.Node;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
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
			persistence.applyNewValue(event.getChangeLog(), new Path(nodeName), -1, payload);
		} else {
			persistence.applyNewValue(event.getChangeLog(), path, -1, payload);
		}
	}

}
