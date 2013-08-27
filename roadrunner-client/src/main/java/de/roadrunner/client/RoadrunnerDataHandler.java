package de.roadrunner.client;

import org.json.Node;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

public class RoadrunnerDataHandler implements Handler<Buffer> {

	private Roadrunner roadrunner;

	public RoadrunnerDataHandler(Roadrunner roadrunner) {
		this.roadrunner = roadrunner;
	}

	@Override
	public void handle(Buffer buffer) {
		Node event = new Node(buffer.toString());
		roadrunner.handleEvent(event);
	}
}
