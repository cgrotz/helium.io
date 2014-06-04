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

package io.helium.persistence.inmemory;

import io.helium.json.Node;
import io.helium.persistence.DataSnapshot;

public class InMemoryDataSnapshot implements DataSnapshot {

    Object val;
    private Node node;

    public InMemoryDataSnapshot(Object value) {
        val = value;
        if (value instanceof Node) {
            this.node = (Node) value;
        }
    }

    @Override
    public Object val() {
        return val;
    }

    @Override
    public DataSnapshot child(String childPath) {
        return new InMemoryDataSnapshot(node.get(childPath));
    }

    @Override
    public boolean hasChild(String childPath) {
        return node.has(childPath);
    }

    @Override
    public boolean hasChildren(String... childPaths) {
        boolean hasChildren = true;
        for (String childPath : childPaths) {
            if (!hasChild(childPath)) {
                hasChildren = false;
            }
        }
        return hasChildren;
    }

    @Override
    public boolean exists() {
        return (val != null);
    }

    @Override
    public boolean isNumber() {
        return val instanceof Integer || val instanceof Float || val instanceof Double;
    }

    @Override
    public boolean isString() {
        return val instanceof String;
    }

    @Override
    public boolean isBoolean() {
        return val instanceof Boolean;
    }

}
