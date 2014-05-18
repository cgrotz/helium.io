package io.helium.event.builder;

import java.util.Date;

import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.Node;

public class HeliumEventBuilder {

	private HeliumEvent	underConstruction;

	private HeliumEventBuilder() {
		this.underConstruction = new HeliumEvent();
	}

	public static HeliumEventBuilder start() {
		return new HeliumEventBuilder();
	}

	public HeliumEvent build() {
		if (!this.underConstruction.has(HeliumEvent.AUTH)) {
			this.underConstruction.put(HeliumEvent.AUTH, new Node());
		}
		return this.underConstruction;
	}

	public Node withNode() {
		Node node = new Node(this);
		underConstruction.put(HeliumEvent.PAYLOAD, node);
		return node;
	}

	public HeliumEventBuilder withPayload(Object payload) {
		underConstruction.put(HeliumEvent.PAYLOAD, payload);
		return this;
	}

	public HeliumEventBuilder creationDate(Date date) {
		underConstruction.put(HeliumEvent.CREATION_DATE, date);
		return this;
	}

	public HeliumEventBuilder type(HeliumEventType type) {
		underConstruction.put(HeliumEvent.TYPE, type.toString());
		return this;
	}

	public HeliumEventBuilder path(String path) {
		underConstruction.put(HeliumEvent.PATH, path);
		return this;
	}

	public HeliumEventBuilder fromHistory(String fromHistory) {
		underConstruction.put(HeliumEvent.FROM_HISTORY, fromHistory);
		return this;
	}

	public HeliumEventBuilder auth(String auth) {
		underConstruction.put(HeliumEvent.FROM_HISTORY, auth);
		return this;
	}

	public HeliumEventBuilder name(String name) {
		underConstruction.put(HeliumEvent.NAME, name);
		return this;
	}

	public HeliumEventBuilder priority(String priority) {
		underConstruction.put(HeliumEvent.PRIORITY, priority);
		return this;
	}

	public HeliumEventBuilder prevChildName(String prevChildName) {
		underConstruction.put(HeliumEvent.PREVCHILDNAME, prevChildName);
		return this;
	}
}
