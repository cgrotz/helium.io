package de.roadrunner.client;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import de.skiptag.roadrunner.json.Node;

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
