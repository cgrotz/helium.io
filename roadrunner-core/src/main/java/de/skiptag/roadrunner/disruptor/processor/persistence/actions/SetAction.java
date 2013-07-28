package de.skiptag.roadrunner.disruptor.processor.persistence.actions;

import org.json.Node;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;

public class SetAction {

    private Persistence persistence;

    public SetAction(Persistence persistence) {
	this.persistence = persistence;
    }

    public void handle(RoadrunnerEvent event) {
	Path path = new Path(event.extractNodePath());
	Node payload;
	if (event.has(RoadrunnerEvent.PAYLOAD)) {
	    Object obj = event.get(RoadrunnerEvent.PAYLOAD);
	    if (obj == Node.NULL || obj == null) {
		event.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
		persistence.remove(event.getChangeLog(), path);
	    } else if (obj instanceof Node) {
		payload = (Node) obj;
		if (payload instanceof Node) {
		    if (event.hasPriority()) {
			persistence.applyNewValue(event.getChangeLog(), path, event.getPriority(), obj);
		    } else {
			persistence.applyNewValue(event.getChangeLog(), path, -1, obj);
		    }
		}
	    } else if (obj == null || obj == Node.NULL) {
		event.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
		persistence.remove(event.getChangeLog(), path);
	    } else {
		if (event.hasPriority()) {
		    persistence.applyNewValue(event.getChangeLog(), path, event.getPriority(), obj);
		} else {
		    persistence.applyNewValue(event.getChangeLog(), path, -1, obj);
		}
	    }
	} else {
	    event.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
	    persistence.remove(event.getChangeLog(), path);
	}

    }

}
