package io.helium.disruptor.processor.persistence.actions;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.persistence.Persistence;

public class SetPriorityAction {

	private Persistence persistence;

	public SetPriorityAction(Persistence persistence) {
		this.persistence = persistence;
	}

	public void handle(HeliumEvent event) {
		Path path = event.extractNodePath();
		int priority = event.getPriority();
		persistence.setPriority(event.getChangeLog(), event.getAuth(), path, priority);
	}

}
