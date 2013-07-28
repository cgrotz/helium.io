package de.skiptag.roadrunner.disruptor.event.changelog;

import de.skiptag.roadrunner.persistence.Path;

public class ValueChangedLogEvent implements ChangeLogEvent {
    private String name;
    private Path path;
    private Path parent;
    private Object value;
    private String prevChildName;
    private int priority;

    public ValueChangedLogEvent(String name, Path path, Path parent,
	    Object value, String prevChildName, int priority) {
	this.name = name;
	this.path = path;
	this.parent = parent;
	this.value = value;
	this.prevChildName = prevChildName;
	this.priority = priority;
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
}
