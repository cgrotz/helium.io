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

package io.helium.persistence.authorization;

import io.helium.common.Path;
import io.helium.json.Node;
import io.helium.persistence.DataSnapshot;

public interface Authorization {
    public static final Node ALL_ACCESS_RULE = new Node(
            "{rules:{\".write\": \"true\",\".read\": \"true\"}}");

    void authorize(Operation op, Node auth, DataSnapshot root, Path path,
                   Object object) throws NotAuthorizedException;

    boolean isAuthorized(Operation op, Node auth, DataSnapshot root, Path path,
                         Object object);
}
