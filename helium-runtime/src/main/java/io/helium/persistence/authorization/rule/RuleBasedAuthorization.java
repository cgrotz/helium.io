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

package io.helium.persistence.authorization.rule;

import io.helium.common.Path;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.persistence.DataSnapshot;
import io.helium.persistence.Persistence;
import io.helium.persistence.SandBoxedScriptingEnvironment;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.NotAuthorizedException;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;

import java.util.Optional;

public class RuleBasedAuthorization implements Authorization {
    private final Persistence persistence;
    private SandBoxedScriptingEnvironment scriptingEnvironment;

    public RuleBasedAuthorization(Persistence persistence) {
        this.persistence = persistence;
        this.scriptingEnvironment = new SandBoxedScriptingEnvironment();
    }

    @Override
    public void authorize(Operation op, Optional<Node> auth, DataSnapshot root, Path path,
                          Object data) throws NotAuthorizedException {
        if (!isAuthorized(op, auth, root, path, data)) {
            throw new NotAuthorizedException(op, path);
        }
    }

    @Override
    public boolean isAuthorized(Operation op, Optional<Node> auth, DataSnapshot root, Path path,
                                Object data) {

        Node localAuth = auth.orElseGet(() -> Authorization.ANONYMOUS);
        RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(persistence.getNode(Path.of("/rules")));
        if (localAuth.has("permissions")) {
            RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getNode("permissions"));
            if( evaluateRules(op, root, path, data, localAuth, userRules) ) {
                return true;
            }
        }
        return evaluateRules(op, root, path, data, localAuth, globalRules);
    }

    private boolean evaluateRules(Operation op, DataSnapshot root, Path path, Object data, Node localAuth, RuleBasedAuthorizator globalRules) {
        String expression = globalRules.getExpressionForPathAndOperation(path, op);
        if("false".equalsIgnoreCase(expression) || "true".equalsIgnoreCase(expression)) {
            return Boolean.parseBoolean(expression);
        }
        else {
            Object evaledAuth = scriptingEnvironment.eval(localAuth.toString());
            Object evaledData = null;
            if(data instanceof HashMapBackedNode) {
                evaledAuth = scriptingEnvironment.eval(data.toString());
            }
            else {
                evaledData  = data;
            }
            Boolean result = (Boolean) scriptingEnvironment.invokeFunction(expression, evaledAuth, path, evaledData, root);
            return result.booleanValue();
        }
    }

    @Override
    public Object filterContent(Optional<Node> auth, Path path, Node root, Object content) {
        if (content instanceof HashMapBackedNode) {
            Node org = (Node) content;
            Node node = new HashMapBackedNode();
            for (String key : org.keys()) {
                if (isAuthorized(Operation.READ, auth, new InMemoryDataSnapshot(root),
                        path.append(key), new InMemoryDataSnapshot(org.get(key)))) {
                    node.put(key, filterContent(auth, path.append(key), root, org.get(key)));
                }
            }
            return node;
        } else {
            return content;
        }
    }
}