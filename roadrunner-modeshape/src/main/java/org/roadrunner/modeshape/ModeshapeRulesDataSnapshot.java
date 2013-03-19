package org.roadrunner.modeshape;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.json.JSONException;
import org.roadrunner.core.authorization.RulesDataSnapshot;

public class ModeshapeRulesDataSnapshot implements RulesDataSnapshot {

	private Node node;

	public ModeshapeRulesDataSnapshot(Node node) {
		this.node = node;
	}

	@Override
	public Object val() {
		try {
			return ModeShapeDataService.transformToJSON(node);
		} catch (ValueFormatException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public RulesDataSnapshot child(String childPath) {
		try {
			return new ModeshapeRulesDataSnapshot(node.getNode(childPath));
		} catch (PathNotFoundException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean hasChild(String childPath) {
		try {
			return node.hasNode(childPath);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean hasChildren(String... childPaths) {
		try {
			if (childPaths.length == 0) {
				return node.hasNodes();
			}
			for(String childPath : childPaths)
			{
				if(!hasChild(childPath))
				{
					return false;
				}
			}
			return true;
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public int getPriority() {
		try {
			return node.getIndex();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public boolean isNumber() {
		return false;
	}

	@Override
	public boolean isString() {
		return false;
	}

	@Override
	public boolean isBoolean() {
		return false;
	}

}
