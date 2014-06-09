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

package io.helium.server.channels.websocket.rpc;

import com.google.common.collect.Maps;
import io.helium.server.channels.websocket.WebsocketEndpoint;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.lang.annotation.*;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Rpc {

    private Map<String, RpcMethodInstance> methods = Maps.newHashMap();
    private final Container container;

    public Rpc(Container container) {
        this.container = container;
    }

    public void register(Object obj) {
        try {
            for (java.lang.reflect.Method method : obj.getClass().getMethods()) {
                if (method.isAnnotationPresent(Method.class)) {
                    methods.put(method.getName(), new RpcMethodInstance(obj, method));
                }
            }
        } catch (Exception e) {
            container.logger().error("Couldn't register object", e);
        }
    }

    public void handle(String message, WebsocketEndpoint socket) {
        JsonObject json = new JsonObject(message);
        String id = checkNotNull(json.getString("id"));
        String method = checkNotNull(json.getString("method"));
        JsonObject args = json.getObject("args");
        if (methods.containsKey(method)) {
            JsonObject response = new JsonObject();
            response.putValue("id", id);
            try {
                response.putValue("resp", methods.get(method).call(args));
                response.putValue("state", "ok");
                response.putValue("type", "rpc");
            } catch (Exception e) {
                this.container.logger().error("RPC failed", e);
                response.putValue("resp", e.getMessage());
                response.putValue("state", "error");
                response.putValue("type", "rpc");
            }
            socket.send(response.toString());
        } else {
            JsonObject response = new JsonObject();
            response.putValue("id", id);
            response.putValue("type", "rpc");
            response.putValue("state", "error");
            response.putValue("resp", method + " not found");
            socket.send(response.toString());
        }
    }

    @Documented
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.METHOD)
    public @interface Method {
    }

    @Documented
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.PARAMETER)
    public @interface Param {
        String value();

        String defaultValue() default "";
    }
}