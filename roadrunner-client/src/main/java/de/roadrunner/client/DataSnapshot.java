package de.roadrunner.client;

import org.json.Node;

public class DataSnapshot {

	private Node event;

	public DataSnapshot(Node event) {
		this.event = event;
	}

	public Object val() {
		return event.get("payload");
	}

	public Roadrunner child(String child) {
		return new Roadrunner(event.getString("path") + "/" + child);
	}

	public String name() {
		return event.getString("name");
	}

	public int numChildren() {
		return event.getInt("numChildren");
	}

	public Roadrunner ref() {
		return new Roadrunner(event.getString("path"));
	}

	public int getPriority() {
		return event.getInt("priority");
	}

}
