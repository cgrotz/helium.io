package io.helium.authorization.rulebased;

import io.helium.authorization.HeliumOperation;
import io.helium.common.Path;
import io.helium.json.Node;

public class RuleBasedAuthorizator {

	private Node	rule;

	public RuleBasedAuthorizator(Node rule) {
		this.rule = rule;
	}

	// private static HeliumOperation getOperation(String key) {
	// for (HeliumOperation operation : HeliumOperation.values()) {
	// if (key.contains(operation.getOp())) {
	// return operation;
	// }
	// }
	// throw new RuntimeException("HeliumOperation " + key + " not found");
	// }

	public String getExpressionForPathAndOperation(Path path, HeliumOperation op) {
		Node node = rule.getLastLeafNode(path);
		if (node != null && node.has(op.getOp()) && node.get(op.getOp()) != null) {
			Object value = node.get(op.getOp());
			return value.toString();
			// return node.getString(op.getOp());
		} else {
			return "true";
		}

	}
}
