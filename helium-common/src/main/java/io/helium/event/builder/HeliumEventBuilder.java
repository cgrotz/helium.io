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

package io.helium.event.builder;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;

import java.util.Date;

public class HeliumEventBuilder {

    private HeliumEvent underConstruction;

    private HeliumEventBuilder() {
        this.underConstruction = new HeliumEvent();
    }

    public static HeliumEventBuilder start() {
        return new HeliumEventBuilder();
    }

    public HeliumEvent build() {
        if (!this.underConstruction.has(HeliumEvent.AUTH)) {
            this.underConstruction.put(HeliumEvent.AUTH, new HashMapBackedNode());
        }
        return this.underConstruction;
    }

    public Node withNode() {
        Node node = new HeliumEvent(this);
        underConstruction.put(HeliumEvent.PAYLOAD, node);
        return node;
    }

    public HeliumEventBuilder withPayload(Object payload) {
        underConstruction.put(HeliumEvent.PAYLOAD, payload);
        return this;
    }

    public HeliumEventBuilder creationDate(Date date) {
        underConstruction.put(HeliumEvent.CREATION_DATE, date);
        return this;
    }

    public HeliumEventBuilder type(HeliumEventType type) {
        underConstruction.put(HeliumEvent.TYPE, type.toString());
        return this;
    }

    public HeliumEventBuilder path(String path) {
        underConstruction.put(HeliumEvent.PATH, path);
        return this;
    }

    public HeliumEventBuilder fromHistory() {
        underConstruction.put(HeliumEvent.FROM_HISTORY, true);
        return this;
    }

    public HeliumEventBuilder auth(String auth) {
        underConstruction.put(HeliumEvent.AUTH, auth);
        return this;
    }

    public HeliumEventBuilder name(String name) {
        underConstruction.put(HeliumEvent.NAME, name);
        return this;
    }

    public HeliumEventBuilder priority(String priority) {
        underConstruction.put(HeliumEvent.PRIORITY, priority);
        return this;
    }

    public HeliumEventBuilder noAuthCheck() {
        underConstruction.put(HeliumEvent.NO_AUTH, true);
        return this;
    }

    public HeliumEventBuilder prevChildName(String prevChildName) {
        underConstruction.put(HeliumEvent.PREVCHILDNAME, prevChildName);
        return this;
    }

    public static HeliumEventBuilder set(Path path, Object value) {
        return start().type(HeliumEventType.SET).path(path.toString()).withPayload(value);
    }

    public static HeliumEventBuilder push(Path path, Object value) {
        return start().type(HeliumEventType.PUSH).path(path.toString()).withPayload(value);
    }
}
