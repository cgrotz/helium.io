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

package io.helium.server.protocols.websocket.rpc;

import com.google.common.collect.Maps;
import io.helium.json.Node;
import io.helium.server.protocols.websocket.WebsocketEndpoint;

import java.lang.annotation.*;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Rpc {

    private Map<String, RpcMethodInstance> methods = Maps.newHashMap();

    public void register(Object obj) {
        try {
            for (java.lang.reflect.Method method : obj.getClass().getMethods()) {
                if (method.isAnnotationPresent(Method.class)) {
                    methods.put(method.getName(), new RpcMethodInstance(obj, method));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handle(String message, WebsocketEndpoint socket) {
        Node json = new Node(message);
        String id = checkNotNull(json.getString("id"));
        String method = checkNotNull(json.getString("method"));
        Node args = json.getNode("args");
        if (methods.containsKey(method)) {
            Node response = new Node();
            response.put("id", id);
            try {
                response.put("resp", methods.get(method).call(args));
                response.put("state", "ok");
                response.put("type", "rpc");
            } catch (Exception e) {
                e.printStackTrace();
                response.put("resp", e.getMessage());
                response.put("state", "error");
                response.put("type", "rpc");
            }
            socket.send(response.toString());
        } else {
            Node response = new Node();
            response.put("id", id);
            response.put("type", "rpc");
            response.put("state", "error");
            response.put("resp", method + " not found");
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