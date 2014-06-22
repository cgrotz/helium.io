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
 * License for the specific language governing rules and limitations
 * under the License.
 */

package io.helium.authorization;

import com.google.common.collect.Maps;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.common.SandBoxedScriptingEnvironment;
import io.helium.persistence.mapdb.MapDbService;
import io.helium.persistence.mapdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import javax.script.ScriptException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Authorizator {
    private static final Logger logger = LoggerFactory.getLogger(SandBoxedScriptingEnvironment.class);
    private final static JsonObject ANONYMOUS = new JsonObject().putBoolean("isAnonymous", true);

    private SandBoxedScriptingEnvironment scriptingEnvironment = new SandBoxedScriptingEnvironment();
    private Map<String, String> functions = Maps.newHashMap();

    private static final Authorizator instance = new Authorizator();

    private Authorizator() {
    }

    public static Authorizator get() {
        return instance;
    }

    public void check(Operation operation, Optional<JsonObject> auth, Path path, Object value, Handler<Boolean> handler) {
        JsonObject localAuth = auth.orElse(ANONYMOUS);
        try {
            RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(MapDbService.get().of(Path.of("/rules")));
            if (localAuth.containsField("rules")) {
                RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getObject("rules"));
                handler.handle(evaluateRules(operation, path, value, localAuth, userRules));
            }
            else {
                handler.handle(evaluateRules(operation, path, value, localAuth, globalRules));
            }
        } catch (NoSuchMethodException | ScriptException e) {
            handler.handle(Boolean.FALSE);
        }
    }

    public void validate(Optional<JsonObject> auth, Path path, Object value, Handler<Object> handler) {
        JsonObject localAuth = auth.orElse(ANONYMOUS);
        try {
            RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(MapDbService.get().of(Path.of("/rules")));
            if (localAuth.containsField("rules")) {
                RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getObject("rules"));
                handler.handle(evaluateValidation(Operation.VALIDATE, path, value, localAuth, userRules));
            }
            else {
                handler.handle(evaluateValidation(Operation.VALIDATE, path, value, localAuth, globalRules));
            }
        } catch (NoSuchMethodException | ScriptException e) {
            handler.handle(value);
        }
    }

    public void filter(Optional<JsonObject> auth, Path path, Object payload, Handler<Object> handler) {
        try {
            handler.handle(filterContent(auth, path, payload));
        } catch (NoSuchMethodException | ScriptException e) {
            logger.error("failed filtering", e);
        }
    }

    private boolean evaluateRules(Operation op, Path path, Object data, JsonObject localAuth, RuleBasedAuthorizator rules) throws ScriptException, NoSuchMethodException {
        String expression = rules.getExpressionForPathAndOperation(path, op);
        if ("false".equalsIgnoreCase(expression)) {
            return false;
        } else if ("true".equalsIgnoreCase(expression)) {
            return true;

        } else {
            Object evaledAuth = eval(localAuth.toString());
            Boolean result = (Boolean) invoke(expression, evaledAuth, path, data);
            if (result == null) {
                return false;
            }
            return result.booleanValue();
        }
    }

    private Object evaluateValidation(Operation op, Path path, Object data, JsonObject localAuth, RuleBasedAuthorizator rules) throws ScriptException, NoSuchMethodException {
        String expression = rules.getExpressionForPathAndOperation(path, op);
        if(expression.equals("false")) {
            return data;
        }
        else {
            Object evaledAuth = eval(localAuth.toString());
            return invoke(expression, evaledAuth, path, data);
        }
    }

    private Object eval(String code) throws ScriptException, NoSuchMethodException {
        scriptingEnvironment.eval("function convert(){ return " + code + ";}");
        return scriptingEnvironment.invokeFunction("convert");
    }

    private Object invoke(String code, Object evaledAuth, Path path, Object data) throws ScriptException, NoSuchMethodException {
        String functionName;
        if (functions.containsKey(code)) {
            functionName = functions.get(code);
        } else {
            functionName = "rule" + UUID.randomUUID().toString().replaceAll("-", "");
            scriptingEnvironment.eval("var " + functionName + " = " + code + ";");
            functions.put(code, functionName);
        }
        return scriptingEnvironment.invokeFunction(functionName, evaledAuth, path, new DataSnapshot(data));
    }

    private Object filterContent(Optional<JsonObject> auth, Path path, Object content) throws ScriptException, NoSuchMethodException {
        if (content instanceof JsonObject) {
            JsonObject org = (JsonObject) content;
            JsonObject node = new JsonObject();
            for (String key : org.getFieldNames()) {
                Object value = org.getValue(key);
                Operation operation = Operation.READ;
                JsonObject localAuth = auth.orElse(ANONYMOUS);

                RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(MapDbService.get().of(Path.of("/rules")));
                if (localAuth.containsField("rules")) {
                    RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getObject("rules"));
                    if (evaluateRules(operation, path, value, localAuth, userRules)) {
                        node.putValue(key, filterContent(auth, path.append(key), value));
                    }
                } else if (evaluateRules(operation, path, value, localAuth, globalRules)) {
                    node.putValue(key, filterContent(auth, path.append(key), value));
                }
            }
            return node;
        } else {
            Operation operation = Operation.READ;
            JsonObject localAuth = auth.orElse(ANONYMOUS);

            RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(MapDbService.get().of(Path.of("/rules")));
            if (localAuth.containsField("rules")) {
                RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getObject("rules"));
                if (evaluateRules(operation, path, content, localAuth, userRules)) {
                    return content;
                }
            } else if (evaluateRules(operation, path, content, localAuth, globalRules)) {
                return content;
            }
        }
        return null;
    }

    public static JsonObject decode(String authorizationToken) {
        String decodedAuthorizationToken = new String(Base64.getDecoder().decode(authorizationToken.substring(6)));
        String username = decodedAuthorizationToken.substring(0, decodedAuthorizationToken.indexOf(":"));
        String password = decodedAuthorizationToken.substring(decodedAuthorizationToken.indexOf(":") + 1);

        return new JsonObject().putString("username", username).putString("password", password);
    }
}
