package de.helium.client;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import io.helium.json.Node;

public class HeliumDataHandler implements Handler<Buffer> {

	private Helium helium;

	public HeliumDataHandler(Helium helium) {
		this.helium = helium;
	}

	@Override
	public void handle(Buffer buffer) {
		Node event = new Node(buffer.toString());
		helium.handleEvent(event);
	}
}
