package de.skiptag.roadrunner.disruptor.event.changelog;

import de.skiptag.roadrunner.persistence.Path;

public class ChildRemovedLogEvent implements ChangeLogEvent {

    private Object value;
    private String name;
    private Path path;

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

}
