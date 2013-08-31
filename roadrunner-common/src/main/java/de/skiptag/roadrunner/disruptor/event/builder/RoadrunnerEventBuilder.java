package de.skiptag.roadrunner.disruptor.event.builder;

import java.util.Date;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.json.Node;

public class RoadrunnerEventBuilder {

	private RoadrunnerEvent	underConstruction;

	private RoadrunnerEventBuilder() {
		this.underConstruction = new RoadrunnerEvent();
	}

	public static RoadrunnerEventBuilder start() {
		return new RoadrunnerEventBuilder();
	}

	public RoadrunnerEvent build() {
		if (!this.underConstruction.has(RoadrunnerEvent.AUTH)) {
			this.underConstruction.put(RoadrunnerEvent.AUTH, new Node());
		}
		return this.underConstruction;
	}

	public Node withNode() {
		Node node = new Node(this);
		underConstruction.put(RoadrunnerEvent.PAYLOAD, node);
		return node;
	}

	public RoadrunnerEventBuilder withPayload(Object payload) {
		underConstruction.put(RoadrunnerEvent.PAYLOAD, payload);
		return this;
	}

	public RoadrunnerEventBuilder creationDate(Date date) {
		underConstruction.put(RoadrunnerEvent.CREATION_DATE, date);
		return this;
	}

	public RoadrunnerEventBuilder type(RoadrunnerEventType type) {
		underConstruction.put(RoadrunnerEvent.TYPE, type.toString());
		return this;
	}

	public RoadrunnerEventBuilder path(String path) {
		underConstruction.put(RoadrunnerEvent.PATH, path);
		return this;
	}

	public RoadrunnerEventBuilder fromHistory(String fromHistory) {
		underConstruction.put(RoadrunnerEvent.FROM_HISTORY, fromHistory);
		return this;
	}

	public RoadrunnerEventBuilder auth(String auth) {
		underConstruction.put(RoadrunnerEvent.FROM_HISTORY, auth);
		return this;
	}

	public RoadrunnerEventBuilder name(String name) {
		underConstruction.put(RoadrunnerEvent.NAME, name);
		return this;
	}

	public RoadrunnerEventBuilder priority(String priority) {
		underConstruction.put(RoadrunnerEvent.PRIORITY, priority);
		return this;
	}

	public RoadrunnerEventBuilder prevChildName(String prevChildName) {
		underConstruction.put(RoadrunnerEvent.PREVCHILDNAME, prevChildName);
		return this;
	}
}
