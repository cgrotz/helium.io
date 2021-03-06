/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium.event.changelog;

import io.helium.common.Path;
import io.helium.persistence.mapdb.Node;

public class ChangeLogBuilder {
    private Node node;
    private Path parentPath;
    private Path path;
    private ChangeLog log;

    public ChangeLogBuilder(ChangeLog log, Path path, Path parentPath, Node node) {
        this.log = log;
        this.path = path;
        this.parentPath = parentPath;
        this.node = node;
    }

    public ChangeLogBuilder getChildLogBuilder(String childName) {
        return new ChangeLogBuilder(log, path.append(childName), path,
                node.getNode(childName));
    }

    public void addChange(String name, Object value) {
        log.addChildChangedLogEntry(name, path, parentPath, value, childCount(value));
        log.addValueChangedLogEntry(name, path.append(name), path, value);
    }

    public void addNew(String name, Object value) {
        log.addChildAddedLogEntry(name, path, parentPath, value, childCount(value));
    }

    public void addChangedNode(String name, Node value) {
        log.addChildChangedLogEntry(name, path, parentPath, value, childCount(value));
    }

    public void addDeleted(String name, Object value) {
        log.addChildDeletedLogEntry(path, name, value);
    }

    private long childCount(Object node) {
        return (node instanceof Node) ? ((Node) node).getChildren().size() : 0;
    }

}
