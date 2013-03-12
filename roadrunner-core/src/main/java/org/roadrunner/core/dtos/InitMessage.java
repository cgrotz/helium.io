package org.roadrunner.core.dtos;

public class InitMessage {

	private String parentPath;
	private String rootPath;
	private String name;

	public InitMessage(String name, String rootPath, String parentPath) {
		this.name = name;
		this.rootPath = rootPath;
		this.parentPath = parentPath;
	}

	public String getParentPath() {
		return parentPath;
	}

	public String getRootPath() {
		return rootPath;
	}

	public String getName() {
		return name;
	}

}
