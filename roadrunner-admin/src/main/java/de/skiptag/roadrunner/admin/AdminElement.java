package de.skiptag.roadrunner.admin;

public class AdminElement {
	private String	name;
	private Object	value;

	public AdminElement(String name, Object value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}
}
