package io.helium.disruptor.processor.persistence.actions;

import java.util.UUID;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.json.Node;
import io.helium.persistence.Persistence;

public class PushAction {

	private Persistence persistence;

	public PushAction(Persistence persistence) {
		this.persistence = persistence;
	}

	public void handle(HeliumEvent event) {
		Path path = event.extractNodePath();
		Object payload;
		if (event.has(HeliumEvent.PAYLOAD)) {
			payload = event.get(HeliumEvent.PAYLOAD);
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
