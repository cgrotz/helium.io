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

package io.helium.client;

import io.helium.json.Node;

public class DataSnapshot {

    private Node event;

    public DataSnapshot(Node event) {
        this.event = event;
    }

    public Object val() {
        return event.get("payload");
    }

    public Helium child(String child) {
        return new Helium(event.getString("path") + "/" + child);
    }

    public String name() {
        return event.getString("name");
    }

    public int numChildren() {
        return event.getInt("numChildren");
    }

    public Helium ref() {
        return new Helium(event.getString("path"));
    }

    public int getPriority() {
        return event.getInt("priority");
    }

}
