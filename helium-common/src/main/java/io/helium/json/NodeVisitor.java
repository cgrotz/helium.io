package io.helium.json;

import io.helium.common.Path;

public interface NodeVisitor {
	public void visitNode(Path path, Node node);

	public void visitProperty(Path path, Node node, String key, Object value);
}
