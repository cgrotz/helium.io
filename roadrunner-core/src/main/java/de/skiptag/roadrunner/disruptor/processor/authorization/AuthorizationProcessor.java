package de.skiptag.roadrunner.disruptor.processor.authorization;

import org.json.Node;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryDataSnapshot;

public class AuthorizationProcessor implements EventHandler<RoadrunnerEvent> {

	private Authorization authorization;

	private Persistence persistence;

	public AuthorizationProcessor(Authorization authorization,
			Persistence persistence) {
		this.authorization = authorization;
		this.persistence = persistence;
	}

	@Override
	public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch) {
		Path path = event.extractNodePath();
		if (event.getType() == RoadrunnerEventType.PUSH) {
			InMemoryDataSnapshot root = new InMemoryDataSnapshot(
					persistence.get(null));
			InMemoryDataSnapshot data = new InMemoryDataSnapshot(
					event.has(RoadrunnerEvent.PAYLOAD) ? event.get(RoadrunnerEvent.PAYLOAD)
							: null);
			authorization.authorize(RoadrunnerOperation.WRITE, getAuth(event), root, path, data);
		} else if (event.getType() == RoadrunnerEventType.SET) {
			if (event.has(RoadrunnerEvent.PAYLOAD)
					&& event.get(RoadrunnerEvent.PAYLOAD) == Node.NULL) {
				InMemoryDataSnapshot root = new InMemoryDataSnapshot(
						persistence.get(null));
				InMemoryDataSnapshot data = new InMemoryDataSnapshot(
						event.get(RoadrunnerEvent.PAYLOAD));
				authorization.authorize(RoadrunnerOperation.WRITE, getAuth(event), root, path, data);
			} else {
				InMemoryDataSnapshot root = new InMemoryDataSnapshot(
						persistence.get(null));
				authorization.authorize(RoadrunnerOperation.WRITE, getAuth(event), root, path, null);
			}
		}
	}

	private Node getAuth(RoadrunnerEvent event) {
		if (event.has(RoadrunnerEvent.AUTH)) {
			return event.getNode(RoadrunnerEvent.AUTH);
		} else {
			return new Node();
		}
	}
}
