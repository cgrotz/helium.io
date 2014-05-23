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

package io.helium.connectivity.rpc;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.helium.connectivity.messaging.HeliumEndpoint;
import io.helium.json.Node;

import java.lang.annotation.*;
import java.util.List;
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

    public void handle(String message, HeliumEndpoint socket) {
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

    private class RpcMethodInstance {
        private Object instance;
        private java.lang.reflect.Method method;
        private ObjectConverter objectConverter = new ObjectConverter();

        public RpcMethodInstance(Object instance, java.lang.reflect.Method method) {
            this.instance = instance;
            this.method = method;
        }

        public Object call(Node passedArgs) {
            try {
                List<Object> args = Lists.newArrayList();
                for (Annotation[] annotations : method.getParameterAnnotations()) {
                    for (Annotation annotation : annotations) {
                        if (annotation instanceof Param) {
                            if (passedArgs != null) {
                                Param param = (Param) annotation;
                                if (passedArgs.has(param.value())) {
                                    args.add(passedArgs.get(param.value()));
                                } else {
                                    if (!Strings.isNullOrEmpty(param.defaultValue())) {
                                        args.add(param.defaultValue());
                                    } else {
                                        args.add(null);
                                    }
                                }
                            }
                        }
                    }
                }
                Object[] argArray = createArray(method, args);
                return method.invoke(instance, argArray);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private Object[] createArray(java.lang.reflect.Method method, List<Object> args) {
            Object[] array = args.toArray();
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                array[i] = objectConverter.convert(array[i], parameterTypes[i]);
            }
            return array;
        }
    }
}