package de.skiptag.roadrunner.core.authorization;

public enum RoadrunnerOperation {
	READ(".read"), WRITE(".write"), REMOVE(".remove");

	private String op;

	RoadrunnerOperation(String op) {
		this.op = op;
	}

	public String getOp() {
		return this.op;
	}
}
