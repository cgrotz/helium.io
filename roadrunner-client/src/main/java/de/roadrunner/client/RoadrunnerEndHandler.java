package de.roadrunner.client;

import org.vertx.java.core.Handler;

public class RoadrunnerEndHandler implements Handler<Void> {

	private Roadrunner roadrunner;

	public RoadrunnerEndHandler(Roadrunner roadrunner) {
		this.roadrunner = roadrunner;
	}

	@Override
	public void handle(Void event) {

	}
}