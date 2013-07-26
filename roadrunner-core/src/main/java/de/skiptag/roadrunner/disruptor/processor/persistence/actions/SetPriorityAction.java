package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class SetPriorityAction {

    private Persistence persistence;

    public SetPriorityAction(Persistence persistence) {
	this.persistence = persistence;
    }

    public void handle(RoadrunnerEvent message) {
	Path path = new Path(message.extractNodePath());
	int priority = message.getPriority();
	persistence.setPriority(path, priority);
    }

}
