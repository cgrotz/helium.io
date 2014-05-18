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

import com.google.common.base.Objects;
import io.helium.common.Path;

public class ValueChangedLogEvent implements ChangeLogEvent {
    private String name;
    private Path path;
    private Path parent;
    private Object value;
    private String prevChildName;
    private int priority;

    public ValueChangedLogEvent(String name, Path path, Path parent, Object value,
                                String prevChildName, int priority) {
        this.name = name;
        this.path = path;
        this.parent = parent;
        this.value = value;
        this.prevChildName = prevChildName;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public Path getParent() {
        return parent;
    }

    public Object getValue() {
        return value;
    }

    public String getPrevChildName() {
        return prevChildName;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("name", name).add("path", path).add("parent", parent)
                .add("value", value).add("prevChildName", prevChildName).add("priority", priority)
                .toString();
    }
}
