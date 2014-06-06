package io.helium.persistence;

import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.persistence.mapdb.MapDbBackedNode;
import io.helium.persistence.mapdb.NodeVisitor;

public class ChildRemovedSubTreeVisitor implements NodeVisitor {
    private ChangeLog log;

    public ChildRemovedSubTreeVisitor(ChangeLog log) {
        this.log = log;
    }

    @Override
    public void visitProperty(Path path, MapDbBackedNode node, String key, Object value) {
        log.addChildRemovedLogEntry(path, key, value);
    }

    @Override
    public void visitNode(Path path, MapDbBackedNode node) {
        log.addChildRemovedLogEntry(path.parent(), path.lastElement(), null);
    }
}