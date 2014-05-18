package de.helium.client;

import io.helium.json.Node;

public class DataSnapshot {

	private Node event;

	public DataSnapshot(Node event) {
		this.event = event;
	}

	public Object val() {
		return event.get("payload");
	}

	public Helium child(String child) {
		return new Helium(event.getString("path") + "/" + child);
	}

	public String name() {
		return event.getString("name");
	}

	public int numChildren() {
		return event.getInt("numChildren");
	}

	public Helium ref() {
		return new Helium(event.getString("path"));
	}

	public int getPriority() {
		return event.getInt("priority");
	}

}
