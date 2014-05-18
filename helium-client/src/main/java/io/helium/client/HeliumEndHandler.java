package de.helium.client;

import org.vertx.java.core.Handler;

public class HeliumEndHandler implements Handler<Void> {

	private Helium helium;

	public HeliumEndHandler(Helium helium) {
		this.helium = helium;
	}

	@Override
	public void handle(Void event) {

	}
}