package io.helium.persistence.mapdb.visitor;

import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.persistence.mapdb.Node;

public class ChildDeletedSubTreeVisitor implements NodeVisitor {
    private ChangeLog log;

    public ChildDeletedSubTreeVisitor(ChangeLog log) {
        this.log = log;
    }

    @Override
    public void visitProperty(Path path, Node node, String key, Object value) {
        log.addChildDeletedLogEntry(path, key, value);
    }

    @Override
    public void visitNode(Path path, Node node) {
        log.addChildDeletedLogEntry(path.parent(), path.lastElement(), null);
    }
}