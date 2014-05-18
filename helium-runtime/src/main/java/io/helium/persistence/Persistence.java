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

import io.helium.authorization.rulebased.RulesDataSnapshot;
import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.messaging.HeliumEndpoint;
import io.helium.queries.QueryEvaluator;

public interface Persistence extends SnapshotProcessor {

    Object get(Path path);

    Node getNode(Path path);

    void remove(ChangeLog log, Node auth, Path path);

    void applyNewValue(ChangeLog log, Node auth, Path path, int priority, Object payload);

    void updateValue(ChangeLog log, Node auth, Path path, int priority, Object payload);

    void setPriority(ChangeLog log, Node auth, Path path, int priority);

    void syncPath(Path path, HeliumEndpoint handler);

    void syncPropertyValue(Path path, HeliumEndpoint heliumEventHandler);

    RulesDataSnapshot getRoot();

    public void syncPathWithQuery(Path path, HeliumEndpoint handler,
                                  QueryEvaluator queryEvaluator, String query);

}