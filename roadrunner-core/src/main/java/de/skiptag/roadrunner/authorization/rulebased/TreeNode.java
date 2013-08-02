package de.skiptag.roadrunner.authorization.rulebased;

import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import de.skiptag.roadrunner.authorization.RoadrunnerOperation;

public class TreeNode {
    Map<RoadrunnerOperation, String> data = Maps.newHashMap();
    private Map<String, TreeNode> children = Maps.newHashMap();

    public TreeNode getNodeCreateIfNecessary(String path) {
	if (path.startsWith("/")) {
	    path = path.substring(1);
	}
	String firstSegment = path.substring(0, path.indexOf("/"));
	String restOfPath = path.substring(path.indexOf("/"));
	TreeNode childNode;
	if (children.containsKey(firstSegment)) {
	    childNode = children.get(firstSegment);
	} else {
	    childNode = new TreeNode();
	    children.put(firstSegment, childNode);
	}
	if (restOfPath.contains("/")) {
	    return childNode.getNodeCreateIfNecessary(restOfPath);
	}
	return this;
    }

    public TreeNode getNode(String path) {
	if (path.startsWith("/")) {
	    path = path.substring(1);
	}
	String firstSegment;
	String restOfPath;
	if (path.contains("/")) {
	    firstSegment = path.substring(0, path.indexOf("/"));
	    restOfPath = path.substring(path.indexOf("/"));
	} else {
	    firstSegment = path;
	    restOfPath = null;
	}
	if (children.containsKey(firstSegment)) {
	    TreeNode childNode = children.get(firstSegment);
	    if (Strings.isNullOrEmpty(restOfPath)) {
		return childNode;
	    } else {
		childNode.getNode(restOfPath);
	    }
	} else {
	    return this;
	}
	return this;
    }

    public void put(RoadrunnerOperation op, String expression) {
	data.put(op, expression);
    }
}