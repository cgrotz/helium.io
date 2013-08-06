package de.skiptag.roadrunner.authorization.rulebased;

import org.json.Node;

import de.skiptag.roadrunner.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.persistence.Path;

public class RuleBasedAuthorizator {

	private Node rule;

	public RuleBasedAuthorizator(Node rule) {
		this.rule = rule;
	}

	private static RoadrunnerOperation getOperation(String key) {
		for (RoadrunnerOperation operation : RoadrunnerOperation.values()) {
			if (key.contains(operation.getOp())) {
				return operation;
			}
		}
		throw new RuntimeException("RoadrunnerOperation " + key + " not found");
	}

	public String getExpressionForPathAndOperation(Path path,
			RoadrunnerOperation op) {
		Node node = rule.getLastLeafNode(path);
		if (node != null && node.has(op.getOp()) && node.get(op.getOp()) != null) {
			return node.getString(op.getOp());
		} else {
			return "true";
		}

	}
}
