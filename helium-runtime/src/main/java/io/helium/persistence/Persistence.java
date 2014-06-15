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

package io.helium.persistence;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.helium.common.Path;
import io.helium.event.builder.HeliumEventBuilder;
import io.helium.persistence.actions.*;
import io.helium.persistence.mapdb.Node;
import io.helium.persistence.mapdb.NodeFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class Persistence extends CommonPersistenceVerticle {

    public static final String PUSH = "io.helium.persistor.push";
    public static final String SET = "io.helium.persistor.set";
    public static final String DELETE = "io.helium.persistor.delete";
    public static final String UPDATE = "io.helium.persistor.update";
    public static final String GET = "io.helium.persistor.get";

    @Override
    public void start(Future<Void> startedResult) {
        NodeFactory.get().dir(new File(container.config().getString("directory")));

        container.deployVerticle(Get.class.getName());
        container.deployVerticle(Push.class.getName());
        container.deployVerticle(Set.class.getName());
        container.deployVerticle(Delete.class.getName());
        container.deployVerticle(Update.class.getName(), new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                try {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    URL demo = cl.getResource("demo.json");
                    String demoData = Resources.toString(demo, Charsets.UTF_8);
                    loadJsonObject(Path.of("/"), new JsonObject(demoData));
                    startedResult.complete();
                }
                catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void loadJsonObject(Path path, JsonObject data) {
        for(String key : data.getFieldNames()) {
            Object value = data.getField(key);
            if(value instanceof JsonObject) {
                loadJsonObject(path.append(key), (JsonObject)value);
            }
            else {
                Node node = Node.of(path);
                node.put(key, value);
            }
        }
        /*
        if (!exists(Path.of("/users"))) {
            String uuid = UUID.randomUUID().toString();
            Node user = Node.of(Path.of("/users").append(uuid.replaceAll("-", "")));
            user.put("username", "admin").put("password", "admin").put("isAdmin", true);
        }

        if (!exists(Path.of("/rules"))) {
            Node rules = Node.of(Path.of("/rules"));
            rules.put(".write", "function(auth, path, data, root){\n" +
                    "   return auth.isAdmin;\n" +
                    "}\n");
            rules.put(".read", true);
            rules.getNode("rules").put(".write", "function(auth, path, data, root){\n" +
                    "  return auth.isAdmin;\n" +
                    "}\n")
                    .put(".read", "function(auth, path, data, root){\n" +
                            "  return auth.isAdmin;\n" +
                            "}\n");
            rules.getNode("users").put(".write", "function(auth, path, data, root){\n" +
                    "  return auth.isAdmin;\n" +
                    "}\n")
                    .put(".read", "function(auth, path, data, root){\n" +
                            "  return auth.isAdmin;\n" +
                            "}\n");
        }*/
    }
}