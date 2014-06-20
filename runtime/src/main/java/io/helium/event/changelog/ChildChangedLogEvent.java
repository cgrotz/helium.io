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
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

public class ChildChangedLogEvent extends ChangeLogEvent {

    public ChildChangedLogEvent(String name, Path path, Path parent, Object value, long numChildren) {
        putString("type", getClass().getSimpleName());
        putString("name", name);
        putString("path", path.toString());
        putString("parent", parent.toString());
        putValue("value", value);
        putNumber("numChildren", numChildren);
    }

    public ChildChangedLogEvent(Map<String, Object> stringObjectMap) {
        super(stringObjectMap);
    }

    public static ChildChangedLogEvent of(JsonObject logE) {
        return new ChildChangedLogEvent(logE.toMap());
    }

    private ChildChangedLogEvent() {

    }

    public String name() {
        return getString("name");
    }

    public Path path() {
        return Path.of(getString("name"));
    }

    public Path parent() {
        return Path.of(getString("parent"));
    }

    public Object value() {
        return getValue("value");
    }

    public int numChildren() {
        return getInteger("numChildren");
    }

    public boolean hasChildren() {
        return (getInteger("numChildren") > 0);
    }
}
