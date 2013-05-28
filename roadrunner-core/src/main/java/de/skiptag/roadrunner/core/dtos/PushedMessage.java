package de.skiptag.roadrunner.core.dtos;

import org.json.JSONObject;

public class PushedMessage {

	public PushedMessage(String parent, String prevChildName,
			JSONObject payload, boolean hasChildren, long numChildren) {
		super();
		this.parent = parent;
		this.prevChildName = prevChildName;
		this.payload = payload;
		this.hasChildren = hasChildren;
		this.numChildren = numChildren;
	}

	private String parent;

	private String prevChildName;

	private JSONObject payload;
	
	private boolean hasChildren;
	
	private long numChildren;
	
	public String getParent() {
		return parent;
	}

	public String getPrevChildName() {
		return prevChildName;
	}

	public JSONObject getPayload() {
		return payload;
	}
	
	public boolean getHasChildren() {
		return hasChildren;
	}

	public long getNumChildren() {
		return numChildren;
	}
}
