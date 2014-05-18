package io.helium.authorization;

public enum HeliumOperation {
	READ(".read"), WRITE(".write");

	private String op;

	HeliumOperation(String op) {
		this.op = op;
	}

	public String getOp() {
		return this.op;
	}
}
