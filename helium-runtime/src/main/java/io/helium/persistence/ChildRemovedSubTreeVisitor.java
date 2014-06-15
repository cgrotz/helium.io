package io.helium.persistence;

import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.persistence.mapdb.Node;
import io.helium.persistence.mapdb.NodeVisitor;

public class ChildRemovedSubTreeVisitor implements NodeVisitor {
    private ChangeLog log;

    public ChildRemovedSubTreeVisitor(ChangeLog log) {
        this.log = log;
    }

    @Override
    public void visitProperty(Path path, Node node, String key, Object value) {
        log.addChildRemovedLogEntry(path, key, value);
    }

    @Override
    public void visitNode(Path path, Node node) {
        log.addChildRemovedLogEntry(path.parent(), path.lastElement(), null);
    }
}