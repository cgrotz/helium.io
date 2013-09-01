package de.skiptag.roadrunner.json;

import de.skiptag.roadrunner.persistence.Path;

public interface NodeVisitor {
	public void visitNode(Path path, Node node);

	public void visitProperty(Path path, Node node, String key, Object value);
}
