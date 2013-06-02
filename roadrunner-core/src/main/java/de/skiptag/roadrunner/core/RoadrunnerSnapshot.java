package de.skiptag.roadrunner.core;

import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.base.Objects;

import de.skiptag.roadrunner.core.authorization.AuthorizationService;

public class RoadrunnerSnapshot {
	private AuthorizationService authorizationService;
	private DataService dataService;
	private String path;
	private JSONObject value;
	private String parentPath;
	private long numChildren;
	private String name;
	private boolean hasChildren;
	private int priority;
	private String contextName;

	public RoadrunnerSnapshot(AuthorizationService authorizationService, DataService dataService, String contextName,
			String path, JSONObject value, String parentPath, long numChildren, String name, boolean hasChildren,
			int priority) {
		super();
		this.authorizationService = authorizationService;
		this.dataService = dataService;
		this.path = path;
		this.contextName = contextName;
		this.value = value;
		this.parentPath = parentPath;
		this.numChildren = numChildren;
		this.name = name;
		this.hasChildren = hasChildren;
		this.priority = priority;
	}

	public RoadrunnerService child(String childPath) {
		return new RoadrunnerService(authorizationService, dataService, contextName, (path.endsWith("/") ? path : path
				+ "/")
				+ childPath);
	}

	public void forEach(Function childAction) {

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
			return new RoadrunnerService(authorizationService, dataService, contextName, parentPath);
		} else {
			return null;
		}
	}

	public String path() {
		return path;
	}

	public RoadrunnerService ref() {
		return new RoadrunnerService(authorizationService, dataService, contextName, path);
	}

	public JSONObject val() {
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
