package de.skiptag.roadrunner.event.changelog;

import com.google.common.base.Objects;

import de.skiptag.roadrunner.common.Path;

public class ChildRemovedLogEvent implements ChangeLogEvent {

	private Object	value;
	private String	name;
	private Path		path;

	public ChildRemovedLogEvent(Path path, String name, Object value) {
		this.path = path;
		this.name = name;
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public Path getPath() {
		return path;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("name", name).add("path", path).add("value", value)
				.toString();
	}
}
