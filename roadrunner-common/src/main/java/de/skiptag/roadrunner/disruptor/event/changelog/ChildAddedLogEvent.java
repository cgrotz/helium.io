package de.skiptag.roadrunner.disruptor.event.changelog;

import com.google.common.base.Objects;

import de.skiptag.roadrunner.persistence.Path;

public class ChildAddedLogEvent implements ChangeLogEvent {
	private String name;
	private Path path;
	private Path parent;
	private Object value;
	private String prevChildName;
	private int priority;
	private long numChildren;

	public ChildAddedLogEvent(String name, Path path, Path parent, Object value, long numChildren,
			String prevChildName, int priority) {
		this.name = name;
		this.path = path;
		this.parent = parent;
		this.value = value;
		this.prevChildName = prevChildName;
		this.priority = priority;
		this.numChildren = numChildren;
	}

	public String getName() {
		return name;
	}

	public Path getPath() {
		return path;
	}

	public Path getParent() {
		return parent;
	}

	public Object getValue() {
		return value;
	}

	public String getPrevChildName() {
		return prevChildName;
	}

	public int getPriority() {
		return priority;
	}

	public long getNumChildren() {
		return this.numChildren;
	}

	public boolean getHasChildren() {
		return this.numChildren > 0;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("name", name).add("path", path)
				.add("parent", parent).add("value", value).add("prevChildName", prevChildName)
				.add("priority", priority).add("numChildren", numChildren).toString();
	}
}
