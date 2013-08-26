package de.skiptag.roadrunner.authorization;

public enum RoadrunnerOperation {
	READ(".read"), WRITE(".write");

	private String op;

	RoadrunnerOperation(String op) {
		this.op = op;
	}

	public String getOp() {
		return this.op;
	}
}
