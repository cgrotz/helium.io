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

import com.google.common.collect.Maps;
import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.SandBoxedScriptingEnvironment;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.NotAuthorizedException;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RuleBasedAuthorization implements Authorization {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedAuthorization.class);
    private final Persistence persistence;
    private SandBoxedScriptingEnvironment scriptingEnvironment;
    private Map<String, String> functions = Maps.newHashMap();

    public RuleBasedAuthorization(Persistence persistence) {
        this.persistence = persistence;
        this.scriptingEnvironment = new SandBoxedScriptingEnvironment();
    }

    @Override
    public void authorize(Operation op, Optional<Node> auth, Path path,
                          Object data) throws NotAuthorizedException {
        if (!isAuthorized(op, auth, path, data)) {
            throw new NotAuthorizedException(op, path);
        }
    }

    @Override
    public boolean isAuthorized(Operation op, Optional<Node> auth, Path path,
                                Object data) {
        try {
            Node localAuth = auth.orElseGet(() -> Authorization.ANONYMOUS);
            RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(persistence.getNode(new ChangeLog(-1), Path.of("/rules")));
            if (localAuth.has("permissions")) {
                RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getNode("permissions"));
                if (evaluateRules(op, path, data, localAuth, userRules)) {
                    return true;
                }
            }
            return evaluateRules(op, path, data, localAuth, globalRules);
        }
        catch (NoSuchMethodException | ScriptException e) {
            LOGGER.error("error evaluating expression for authorization", e);
            return false;
        }
    }

    private boolean evaluateRules(Operation op, Path path, Object data, Node localAuth, RuleBasedAuthorizator globalRules) throws ScriptException, NoSuchMethodException {
        String expression = globalRules.getExpressionForPathAndOperation(path, op);
        if("false".equalsIgnoreCase(expression)) {
            return false;
        }
        else if ("true".equalsIgnoreCase(expression)) {
            return true;

        }
        else {
            Object evaledAuth = eval(localAuth.toString());
            Boolean result = (Boolean) invoke(expression, evaledAuth, path);
            if(result == null) {
                return false;
            }
            return result.booleanValue();
        }
    }

    private Object eval(String code) throws ScriptException, NoSuchMethodException {
        scriptingEnvironment.eval("function convert(){ return " + code + ";}");
        Object retValue =scriptingEnvironment.invokeFunction("convert");
        return retValue;
    }

    private Object invoke(String code, Object evaledAuth, Path path) throws ScriptException, NoSuchMethodException {
        String functionName;
        if ( functions.containsKey(code)) {
            functionName = functions.get(code);
        }
        else {
            functionName = "rule" + UUID.randomUUID().toString().replaceAll("-", "");
            scriptingEnvironment.eval("var " + functionName + " = " + code + ";");
            functions.put(code, functionName);
        }
        return (Boolean) scriptingEnvironment.invokeFunction(functionName, evaledAuth, path);
    }

    @Override
    public Object filterContent(Optional<Node> auth, Path path, Object content) {
        if (content instanceof Node) {
            Node org = (Node) content;
            Node node = new HashMapBackedNode();
            for (String key : org.keys()) {
                if (isAuthorized(Operation.READ, auth,
                        path.append(key), new InMemoryDataSnapshot(org.get(key)))) {
                    node.put(key, filterContent(auth, path.append(key), org.get(key)));
                }
            }
            return node;
        } else {
            if (isAuthorized(Operation.READ, auth,
                    path, new InMemoryDataSnapshot(content))) {
                return content;
            }
        }
        return null;
    }
}