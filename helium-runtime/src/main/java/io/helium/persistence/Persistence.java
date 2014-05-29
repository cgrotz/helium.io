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

package io.helium.persistence;

import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.persistence.queries.QueryEvaluator;
import io.helium.server.protocols.http.HttpEndpoint;

public interface Persistence {

    Object get(Path path);

    Node getNode(Path path);

    void remove(ChangeLog log, Node auth, Path path);

    void applyNewValue(ChangeLog log, Node auth, Path path, int priority, Object payload);

    void updateValue(ChangeLog log, Node auth, Path path, int priority, Object payload);

    void setPriority(ChangeLog log, Node auth, Path path, int priority);

    void syncPath(Path path, HttpEndpoint handler);

    void syncPropertyValue(Path path, HttpEndpoint heliumEventHandler);

    DataSnapshot getRoot();

    void syncPathWithQuery(Path path, HttpEndpoint handler,
                           QueryEvaluator queryEvaluator, String query);
}
