package de.skiptag.roadrunner.api;

import com.google.common.base.Objects;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.persistence.Persistence;

public class RoadrunnerSnapshot {
    private Authorization authorization;
    private Persistence persistence;
    private String path;
    private Object value;
    private String parentPath;
    private long numChildren;
    private String name;
    private boolean hasChildren;
    private int priority;
    private String contextName;

    public RoadrunnerSnapshot(Authorization authorization,
	    Persistence persistence, String contextName, String path,
	    Object value, String parentPath, long numChildren, String name,
	    boolean hasChildren, int priority) {
	super();
	this.authorization = authorization;
	this.persistence = persistence;
	this.path = path.toString();
	this.contextName = contextName;
	this.value = value;
	this.parentPath = parentPath.toString();
	this.numChildren = numChildren;
	this.name = name;
	this.hasChildren = hasChildren;
	this.priority = priority;
    }

    public RoadrunnerService child(String childPath) {
	return new RoadrunnerService(authorization, persistence, contextName,
		(path.endsWith("/") ? path : path + "/") + childPath);
    }

    public int getPriority() {
	return priority;
    }

    public boolean hasChildren() {
	return hasChildren;
    }

    public String name() {
	return name;
    }

    public long numChildren() {
	return numChildren;
    }

    public RoadrunnerService parent() {
	if (parentPath != null) {
	    return new RoadrunnerService(authorization, persistence,
		    contextName, parentPath);
	} else {
	    return null;
	}
    }

    public String path() {
	return path;
    }

    public RoadrunnerService ref() {
	return new RoadrunnerService(authorization, persistence, contextName,
		path);
    }

    public Object val() {
	return value;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	} else if (obj instanceof RoadrunnerSnapshot) {
	    return Objects.equal(path, ((RoadrunnerSnapshot) obj).path);
	} else {
	    return super.equals(obj);
	}
    }

    @Override
    public String toString() {
	return Objects.toStringHelper(this).add("path", path).toString();
    }

}
