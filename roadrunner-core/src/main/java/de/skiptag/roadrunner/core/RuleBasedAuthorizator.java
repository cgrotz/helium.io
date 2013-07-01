package de.skiptag.roadrunner.core;

import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import de.skiptag.roadrunner.core.authorization.RoadrunnerOperation;

public class RuleBasedAuthorizator {

	private TreeNode ruleTree = new TreeNode();

	public RuleBasedAuthorizator(JSONObject rule) throws JSONException {
		parseRuleObject(ruleTree, rule);
	}

	@SuppressWarnings("unchecked")
	private void parseRuleObject(TreeNode node, JSONObject rule)
			throws JSONException {
		Iterator<String> itr = rule.keys();
		while (itr.hasNext()) {
			String key = itr.next();
			Object value = rule.get(key);
			if (value instanceof String) {
				String path = key.substring(0, key.lastIndexOf("."));
				String expression = (String) value;
				RoadrunnerOperation op = getOperation(key);
				if (!Strings.isNullOrEmpty(path)) {
					node.getNodeCreateIfNecessary(path).put(op, expression);
				} else {
					node.put(op, expression);
				}
			} else if (value instanceof JSONObject) {
				parseRuleObject(node.getNodeCreateIfNecessary(key),
						(JSONObject) value);
			} else {
				throw new RuntimeException("Type not accepted");
			}
		}
	}

	public class TreeNode {
		private Map<RoadrunnerOperation, String> data = Maps.newHashMap();
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

	private static RoadrunnerOperation getOperation(String key) {

		for (RoadrunnerOperation operation : RoadrunnerOperation.values()) {
			if (key.contains(operation.getOp())) {
				return operation;
			}
		}
		throw new RuntimeException("RoadrunnerOperation " + key + " not found");
	}

	public String getExpressionForPathAndOperation(String path,
			RoadrunnerOperation op) {
		return ruleTree.getNode(path).data.get(op);
	}
}
