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

    public void handle(RoadrunnerEvent message) {
	Path path = new Path(message.extractNodePath());
	Node payload;
	if (message.has(RoadrunnerEvent.PAYLOAD)) {
	    Object obj = message.get(RoadrunnerEvent.PAYLOAD);
	    if (obj == Node.NULL || obj == null) {
		message.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
		persistence.remove(path);
	    } else if (obj instanceof Node) {
		payload = (Node) obj;
		if (payload instanceof Node) {
		    boolean created;
		    if (message.hasPriority()) {
			created = persistence.applyNewValue(path, message.getPriority(), obj);
		    } else {
			created = persistence.applyNewValue(path, -1, obj);
		    }
		    message.created(created);
		}
	    } else if (obj == null || obj == Node.NULL) {
		message.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
		persistence.remove(path);
	    } else {
		if (message.hasPriority()) {
		    persistence.applyNewValue(path, message.getPriority(), obj);
		} else {
		    persistence.applyNewValue(path, -1, obj);
		}
	    }
	} else {
	    message.put(RoadrunnerEvent.OLD_VALUE, persistence.get(path));
	    persistence.remove(path);
	}

    }

}
