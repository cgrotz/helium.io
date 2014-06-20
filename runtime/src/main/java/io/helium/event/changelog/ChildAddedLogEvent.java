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

public class ChildAddedLogEvent extends ChangeLogEvent {

    public ChildAddedLogEvent(String name, Path path, Path parent, Object value, long numChildren) {
        putString("type", getClass().getSimpleName());
        putString("name", name);
        putString("path", path.toString());
        putString("parent", parent.toString());
        putValue("value", value);
        putNumber("numChildren", numChildren);
    }

    public ChildAddedLogEvent(Map<String, Object> stringObjectMap) {
        super(stringObjectMap);
    }

    public static ChildAddedLogEvent of(JsonObject logE) {
        return new ChildAddedLogEvent(logE.toMap());
    }

    private ChildAddedLogEvent() {

    }


    public String getName() {
        return getString("name");
    }

    public Path getPath() {
        //return Path.of(getString("path")).append(getString("name"));
        return Path.of(getString("path"));
    }

    public Path getParent() {
        return Path.of(getString("parent"));
    }

    public Object getValue() {
        return getValue("value");
    }

    public int getNumChildren() {
        return getInteger("numChildren");
    }

    public boolean getHasChildren() {
        return (getInteger("numChildren") > 0);
    }
}
