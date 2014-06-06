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

public class ChildRemovedLogEvent extends ChangeLogEvent {

    public ChildRemovedLogEvent(Path path, String name, Object value) {
        putString("path", path.toString());
        putString("name", name);
        putValue("value", value);
    }

    public Object getValue() {
        return getValue("value");
    }

    public String getName() {
        return getString("name");
    }

    public Path getPath() {
        return Path.of(getString("path"));
    }

}
