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

package io.helium.authorization;

import com.google.common.collect.Maps;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.persistence.DataSnapshot;
import io.helium.persistence.SandBoxedScriptingEnvironment;
import io.helium.persistence.mapdb.MapDbBackedNode;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import javax.script.ScriptException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Authorizator extends Verticle {
    public final static JsonObject ANONYMOUS = new JsonObject().putBoolean("isAnonymous", true);

    public static final String FILTER = "io.helium.authorizator.filter";
    public static final String CHECK = "io.helium.authorizator.check";

    private SandBoxedScriptingEnvironment scriptingEnvironment;
    private Map<String, String> functions = Maps.newHashMap();

    @Override
    public void start() {
        this.scriptingEnvironment = new SandBoxedScriptingEnvironment(container);
        vertx.eventBus().registerHandler(FILTER, new Handler<Message>() {
            @Override
            public void handle(Message event) {
                filter(event);
            }
        });

        vertx.eventBus().registerHandler(CHECK, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Operation operation = Operation.get(event.body().getString("operation"));
                Optional<JsonObject> auth;
                if (event.body().containsField("auth")) {
                    auth = Optional.of(event.body().getObject("auth"));
                } else {
                    auth = Optional.empty();
                }

                Path path = Path.of(event.body().getString("path"));
                Object value = event.body().getValue("payload");
                JsonObject localAuth = auth.orElse(ANONYMOUS);
                try {
                    RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(MapDbBackedNode.of(Path.of("/rules")));
                    if (localAuth.containsField("permissions")) {
                        RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getObject("permissions"));
                        event.reply(evaluateRules(operation, path, value, localAuth, userRules));
                        return;
                    }
                    event.reply(evaluateRules(operation, path, value, localAuth, globalRules));
                    return;
                } catch (NoSuchMethodException | ScriptException e) {
                    event.reply(Boolean.FALSE);
                }
            }
        });
    }

    private void filter(Message<JsonObject> message) {
        Optional<JsonObject> auth;
        if (message.body().containsField("auth")) {
            auth = Optional.of(message.body().getObject("auth"));
        } else {
            auth = Optional.empty();
        }

        Path path = Path.of(message.body().getString("path"));
        Object payload = message.body().getValue("payload");
        try {
            message.reply(filterContent(auth, path, payload));
        } catch (NoSuchMethodException | ScriptException e) {
            container.logger().error("failed filtering", e);
        }
    }

    private Optional<JsonObject> getAuth(HeliumEvent event) {
        if (event.containsField(HeliumEvent.AUTH)) {
            return Optional.of(event.getObject(HeliumEvent.AUTH));
        } else {
            return Optional.empty();
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
            Boolean result = (Boolean) invoke(expression, evaledAuth, path);
            if (result == null) {
                return false;
            }
            return result.booleanValue();
        }
    }

    private Object eval(String code) throws ScriptException, NoSuchMethodException {
        scriptingEnvironment.eval("function convert(){ return " + code + ";}");
        Object retValue = scriptingEnvironment.invokeFunction("convert");
        return retValue;
    }

    private Object invoke(String code, Object evaledAuth, Path path) throws ScriptException, NoSuchMethodException {
        String functionName;
        if (functions.containsKey(code)) {
            functionName = functions.get(code);
        } else {
            functionName = "rule" + UUID.randomUUID().toString().replaceAll("-", "");
            scriptingEnvironment.eval("var " + functionName + " = " + code + ";");
            functions.put(code, functionName);
        }
        return (Boolean) scriptingEnvironment.invokeFunction(functionName, evaledAuth, path);
    }

    public Object filterContent(Optional<JsonObject> auth, Path path, Object content) throws ScriptException, NoSuchMethodException {
        if (content instanceof JsonObject) {
            JsonObject org = (JsonObject) content;
            JsonObject node = new JsonObject();
            for (String key : org.getFieldNames()) {
                Object value = org.getValue(key);
                Operation operation = Operation.READ;
                JsonObject localAuth = auth.orElse(ANONYMOUS);

                RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(MapDbBackedNode.of(Path.of("/rules")));
                if (localAuth.containsField("permissions")) {
                    RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getObject("permissions"));
                    if (evaluateRules(operation, path, value, localAuth, userRules)) {
                        node.putValue(key, filterContent(auth, path, value));
                    }
                } else if (evaluateRules(operation, path, value, localAuth, globalRules)) {
                    node.putValue(key, filterContent(auth, path, value));
                }
            }
            return node;
        } else {
            Operation operation = Operation.READ;
            JsonObject localAuth = auth.orElse(ANONYMOUS);

            RuleBasedAuthorizator globalRules = new RuleBasedAuthorizator(MapDbBackedNode.of(Path.of("/rules")));
            if (localAuth.containsField("permissions")) {
                RuleBasedAuthorizator userRules = new RuleBasedAuthorizator(localAuth.getObject("permissions"));
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

    public static JsonObject filter(Optional<JsonObject> auth, Path nodePath, Object value) {
        JsonObject jsonObject = new JsonObject()
                .putString("path", nodePath.toString());

        if (value instanceof MapDbBackedNode) {
            jsonObject.putValue("payload", ((MapDbBackedNode) value).toJsonObject());
        } else if (value instanceof DataSnapshot) {
            jsonObject.putValue("payload", ((DataSnapshot) value).val());
        } else {
            jsonObject.putValue("payload", value);
        }


        if (auth.isPresent()) {
            jsonObject.putObject("auth", new JsonObject(auth.get().toString()));
        }
        return jsonObject;
    }

    public static JsonObject check(Operation operation, Optional<JsonObject> auth, Path path, Object value) {
        JsonObject event = new JsonObject().putString("operation", operation.getOp());
        if (auth.isPresent()) {
            event.putObject("auth", auth.get());
        }
        event.putString("path", path.toString());
        if (value instanceof MapDbBackedNode) {
            event.putValue("payload", ((MapDbBackedNode) value).toJsonObject());
        } else if (value instanceof DataSnapshot) {
            event.putValue("payload", ((DataSnapshot) value).val());
        } else {
            event.putValue("payload", value);
        }
        return event;
    }
}
