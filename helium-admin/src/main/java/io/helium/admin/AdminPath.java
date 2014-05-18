package io.helium.admin;

import io.helium.common.Path;

public class AdminPath {

	private String	url;
	private String	name;
	private boolean	active;

	public AdminPath(String basePath, Path path) {
		this.url = basePath + path.toString();
		this.name = path.getLastElement();
		this.active = path.isSimple();
	}

	public String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}

	public boolean isActive() {
		return active;
	}
}
