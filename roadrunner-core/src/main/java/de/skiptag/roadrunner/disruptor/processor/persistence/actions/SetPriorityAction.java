package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import de.skiptag.roadrunner.common.Path;
import de.skiptag.roadrunner.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Persistence;

public class SetPriorityAction {

	private Persistence persistence;

	public SetPriorityAction(Persistence persistence) {
		this.persistence = persistence;
	}

	public void handle(RoadrunnerEvent event) {
		Path path = event.extractNodePath();
		int priority = event.getPriority();
		persistence.setPriority(event.getChangeLog(), event.getAuth(), path, priority);
	}

}
