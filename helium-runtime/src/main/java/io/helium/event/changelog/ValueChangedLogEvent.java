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

public class ValueChangedLogEvent extends ChangeLogEvent {

    public ValueChangedLogEvent(String name, Path path, Path parent, Object value) {
        putString("type", getClass().getSimpleName());
        putString("name", name);
        putString("path", path.toString());
        putString("parent", parent.toString());
        putValue("value", value);
    }

    public ValueChangedLogEvent(Map<String, Object> stringObjectMap) {
        super(stringObjectMap);
    }

    public static ValueChangedLogEvent of(JsonObject logE) {
        return new ValueChangedLogEvent(logE.toMap());
    }

    private ValueChangedLogEvent() {

    }

    public String getName() {
        return getString("name");
    }

    public Path getPath() {
        return Path.of(getString("name"));
    }

    public Path getParent() {
        return Path.of(getString("parent"));
    }

    public Object getValue() {
        return getValue("value");
    }
}