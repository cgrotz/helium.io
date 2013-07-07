package de.skiptag.roadrunner.core.dataService.inmemory;

import org.json.JSONException;

import de.skiptag.roadrunner.core.authorization.rulebased.RulesDataSnapshot;

public class NodeRulesDataSnapshot implements RulesDataSnapshot {

    Object val;
    private Node node;

    public NodeRulesDataSnapshot(Object value) {
	val = value;
	if (value instanceof Node) {
	    this.node = (Node) value;
	}
    }

    @Override
    public Object val() {
	return val;
    }

    @Override
    public RulesDataSnapshot child(String childPath) {
	try {
	    return new NodeRulesDataSnapshot(node.get(childPath));
	} catch (JSONException e) {
	    return null;
	}
    }

    @Override
    public boolean hasChild(String childPath) {
	return node.has(childPath);
    }

    @Override
    public boolean hasChildren(String... childPaths) {
	boolean hasChildren = true;
	for (String childPath : childPaths) {
	    if (!hasChild(childPath)) {
		hasChildren = false;
	    }
	}
	return hasChildren;
    }

    @Override
    public boolean exists() {
	return (val != null);
    }

    @Override
    public int getPriority() {
	return 0;
    }

    @Override
    public boolean isNumber() {
	return val instanceof Integer || val instanceof Float
		|| val instanceof Double;
    }

    @Override
    public boolean isString() {
	return val instanceof String;
    }

    @Override
    public boolean isBoolean() {
	return val instanceof Boolean;
    }

}
