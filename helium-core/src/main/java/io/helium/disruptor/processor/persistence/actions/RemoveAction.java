package io.helium.disruptor.processor.persistence.actions;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.persistence.Persistence;

public class RemoveAction {

	private Persistence	persistence;

	public RemoveAction(Persistence persistence) {
		this.persistence = persistence;
	}

	public void handle(HeliumEvent event) {
		Path path = event.extractNodePath();

		persistence.remove(event.getChangeLog(), event.getAuth(), path);

	}
}
