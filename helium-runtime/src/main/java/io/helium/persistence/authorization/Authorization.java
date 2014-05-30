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
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.persistence.DataSnapshot;

import java.util.Optional;

public interface Authorization {
    public final static Node ANONYMOUS = new HashMapBackedNode().put("isAnonymous",true).put("permissions", new HashMapBackedNode());

    void authorize(Operation op,
                   Optional<Node> auth,
                   DataSnapshot root,
                   Path path,
                   Object object) throws NotAuthorizedException;

    boolean isAuthorized(Operation op,
                         Optional<Node> auth,
                         DataSnapshot root,
                         Path path,
                         Object object);

    Object filterContent(Optional<Node> auth, Path path, Node root, Object content);
}
