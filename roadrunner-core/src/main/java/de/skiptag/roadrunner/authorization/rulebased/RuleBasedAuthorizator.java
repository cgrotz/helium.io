package de.skiptag.roadrunner.authorization.rulebased;

import java.util.Iterator;

import org.json.Node;

import com.google.common.base.Strings;

import de.skiptag.roadrunner.authorization.RoadrunnerOperation;

public class RuleBasedAuthorizator {

    private TreeNode ruleTree = new TreeNode();

    public RuleBasedAuthorizator(Node rule) {
	parseRuleObject(ruleTree, rule);
    }

    private void parseRuleObject(TreeNode node, Node rule) {
	Iterator<String> itr = rule.keyIterator();
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
	    } else if (value instanceof Node) {
		parseRuleObject(node.getNodeCreateIfNecessary(key), (Node) value);
	    } else {
		throw new RuntimeException("Type not accepted");
	    }
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
