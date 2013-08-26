package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import org.json.Node;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class UpdateAction {

	private Persistence persistence;

	public UpdateAction(Persistence persistence) {
		this.persistence = persistence;
	}

	public void handle(RoadrunnerEvent event) {
		Path path = event.extractNodePath();
		Node payload;
		if (event.has(RoadrunnerEvent.PAYLOAD)) {
			Object obj = event.get(RoadrunnerEvent.PAYLOAD);
			if (obj instanceof Node) {
				payload = (Node) obj;
				if (payload instanceof Node) {
					if (event.hasPriority()) {
						persistence.updateValue(event.getChangeLog(), event.getAuth(), path,
								event.getPriority(), obj);
					} else {
						persistence.updateValue(event.getChangeLog(), event.getAuth(), path, -1,
								obj);
					}
				}
			}
		} else {
			persistence.remove(event.getChangeLog(), event.getAuth(), path);
		}

	}

}
