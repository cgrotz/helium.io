package de.skiptag.roadrunner.persistence.inmemory;

import org.json.Node;

import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;

public class InMemoryDataSnapshot implements RulesDataSnapshot {

	Object val;
	private Node node;

	public InMemoryDataSnapshot(Object value) {
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
		return new InMemoryDataSnapshot(node.get(childPath));
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
	public boolean isNumber() {
		return val instanceof Integer || val instanceof Float || val instanceof Double;
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
