package io.helium.disruptor.processor.authorization;

import com.lmax.disruptor.EventHandler;

import io.helium.authorization.Authorization;
import io.helium.authorization.HeliumOperation;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;

public class AuthorizationProcessor implements EventHandler<HeliumEvent> {

	private Authorization authorization;

	private Persistence persistence;

	public AuthorizationProcessor(Authorization authorization,
			Persistence persistence) {
		this.authorization = authorization;
		this.persistence = persistence;
	}

	@Override
	public void onEvent(HeliumEvent event, long sequence, boolean endOfBatch) {
		Path path = event.extractNodePath();
		if (event.getType() == HeliumEventType.PUSH) {
			InMemoryDataSnapshot root = new InMemoryDataSnapshot(
					persistence.get(null));
			InMemoryDataSnapshot data = new InMemoryDataSnapshot(
					event.has(HeliumEvent.PAYLOAD) ? event.get(HeliumEvent.PAYLOAD)
							: null);
			authorization.authorize(HeliumOperation.WRITE, getAuth(event), root, path, data);
		} else if (event.getType() == HeliumEventType.SET) {
			if (event.has(HeliumEvent.PAYLOAD)
					&& event.get(HeliumEvent.PAYLOAD) == Node.NULL) {
				InMemoryDataSnapshot root = new InMemoryDataSnapshot(
						persistence.get(null));
				InMemoryDataSnapshot data = new InMemoryDataSnapshot(
						event.get(HeliumEvent.PAYLOAD));
				authorization.authorize(HeliumOperation.WRITE, getAuth(event), root, path, data);
			} else {
				InMemoryDataSnapshot root = new InMemoryDataSnapshot(
						persistence.get(null));
				authorization.authorize(HeliumOperation.WRITE, getAuth(event), root, path, null);
			}
		}
	}

	private Node getAuth(HeliumEvent event) {
		if (event.has(HeliumEvent.AUTH)) {
			return event.getNode(HeliumEvent.AUTH);
		} else {
			return new Node();
		}
	}
}
