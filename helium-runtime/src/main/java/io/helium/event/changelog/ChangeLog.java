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
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public class ChangeLog {

    private final JsonArray array;

    public ChangeLog(JsonArray body) {
        this.array = body;
    }

    public void addLog(ChangeLogEvent event) {
        array.add(event);
    }

    public void addChildAddedLogEntry(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren) {
        array.add(new ChildAddedLogEvent(name, path, parent, value, numChildren));
    }

    public void addChildChangedLogEntry(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren) {
        if (name != null) {
            array.add(new ChildChangedLogEvent(name, path, parent, value, numChildren));
        }
    }

    public void addValueChangedLogEntry(String name, Path path, Path parent, Object value) {
        array.add(new ValueChangedLogEvent(name, path, parent, value));
    }

    public void addChildRemovedLogEntry(Path path, String name, Object value) {
        array.add(new ChildRemovedLogEvent(path, name, value));
    }

    public static ChangeLog of(JsonArray body) {
        return new ChangeLog(body);
    }

    public void forEach(Handler<Object> handler) {
        array.forEach(element -> {
            handler.handle(element);
        });
    }
}
