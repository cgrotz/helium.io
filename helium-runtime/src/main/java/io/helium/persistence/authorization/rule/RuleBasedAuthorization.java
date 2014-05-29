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
import io.helium.event.HeliumEvent;
import io.helium.json.Node;
import io.helium.persistence.DataSnapshot;
import io.helium.persistence.SandBoxedScriptingEnvironment;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.NotAuthorizedException;
import io.helium.persistence.authorization.Operation;

public class RuleBasedAuthorization implements Authorization {

    private RuleBasedAuthorizator rule;
    private SandBoxedScriptingEnvironment scriptingEnvironment;

    public RuleBasedAuthorization(Node rule) {
        if (rule != null && rule.has("rules")) {
            this.rule = new RuleBasedAuthorizator(rule.getNode("rules"));
        } else {
            this.rule = new RuleBasedAuthorizator(Authorization.ALL_ACCESS_RULE.getNode("rules"));
        }

        this.scriptingEnvironment = new SandBoxedScriptingEnvironment();
    }

    @Override
    public void authorize(Operation op, Node auth, DataSnapshot root, Path path,
                          Object data) throws NotAuthorizedException {
        if (!isAuthorized(op, auth, root, path, data)) {
            throw new NotAuthorizedException(op, path);
        }
    }

    @Override
    public boolean isAuthorized(Operation op, Node auth, DataSnapshot root, Path path,
                                Object data) {
        String expression = rule.getExpressionForPathAndOperation(path, op);
        try {
            return Boolean.parseBoolean(expression);
        } catch (Exception e) {
            scriptingEnvironment.put(HeliumEvent.AUTH, scriptingEnvironment.eval(auth.toString()));
            Boolean result = (Boolean) scriptingEnvironment.eval(expression);
            return result.booleanValue();
        }
    }
}