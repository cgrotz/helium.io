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

package io.helium.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.WebSocket;

import java.net.MalformedURLException;
import java.util.Set;
import java.util.UUID;

public class Helium {

    protected WebSocket ws;
    private String path;
    private Multimap<Tuple<String, String>, HeliumCallback> callbacks = ArrayListMultimap
            .create();
    private Set<String> msgCache = Sets.newHashSet();

    public Helium(String path) {
        this.path = Preconditions.checkNotNull(path);
        try {
            HeliumConnection.getInstance().getSocketFor(path, new Handler<WebSocket>() {
                @Override
                public void handle(WebSocket ws) {
                    Helium.this.ws = ws;
                    ws.endHandler(new HeliumEndHandler(Helium.this));
                    ws.dataHandler(new HeliumDataHandler(Helium.this));
                    for (String msg : msgCache) {
                        send(msg);
                    }
                }
            });
        } catch (MalformedURLException e) {

        }
    }

    public static void main(String args[]) {
        Helium ref = new Helium("http://localhost:8080/drawing/points");
        HeliumCallback callback = new HeliumCallback() {
            @Override
            public void handle(DataSnapshot data, String prevChildName) {
                System.out.println(data.name());
            }
        };
        ref.on("child_added", callback);
        ref.on("child_changed", callback);
        ref.on("child_removed", callback);
        ref.child("11:2").set("000");
        ref.child("12:2").set("000");
        ref.child("13:2").set("000");
        ref.child("14:2").set("000");
        while (true) {

        }
    }

    public void handleEvent(Node event) {
        if (!"rpc".equalsIgnoreCase(event.getString("type"))) {
            String type = event.getString("type");
            String path = event.getString("path");
            Tuple<String, String> tuple = new Tuple<String, String>(type, path);
            for (HeliumCallback callback : callbacks.get(tuple)) {
                DataSnapshot snapshot = new DataSnapshot(event);
                callback.handle(snapshot, null);
            }
        }
    }

    private void send(String msg) {
        if (ws == null) {
            msgCache.add(msg);
        } else {
            ws.writeTextFrame(msg);
        }
    }

    private void send(String type, Object data, String name) {
        Node node = new HashMapBackedNode();
        node.put("type", type);
        node.put("name", name);
        node.put("path", path);
        node.put("payload", data);
        send(node.toString());
    }

    private void sendRpc(String method, Node args) {
        Node node = new HashMapBackedNode();
        node.put("id", UUID.randomUUID().toString());
        node.put("method", method);
        node.put("args", args);
        send(node.toString());
    }

    public Helium child(String child) {
        Preconditions.checkNotNull(child);
        return new Helium(path + "/" + child);
    }

    public Helium parent() {
        return new Helium(path.substring(0, path.lastIndexOf("/")));
    }

    public Helium push(Object value) {
        String name = UUID.randomUUID().toString();
        sendRpc("push", new HashMapBackedNode().put("path", path).put("name", name).put("data", value));
        return new Helium(path + "/" + name);
    }

    public void set(Object data) {
        sendRpc("set", new HashMapBackedNode().put("path", path).put("data", data));
    }

    public void set(String name, Object data) {
        sendRpc("set", new HashMapBackedNode().put("path", path).put("name", name).put("data", data));
    }

    public void update(Object data) {
        sendRpc("update", new HashMapBackedNode().put("path", path).put("data", data));
    }

    public void on(String event_type, HeliumCallback callback) {
        callbacks.put(new Tuple<String, String>(event_type, path), callback);
        sendRpc("attachListener", new HashMapBackedNode().put("path", path).put("event_type", event_type));
    }

    public void off(String event_type, HeliumCallback callback) {
        callbacks.remove(event_type, callback);
        sendRpc("detachListener", new HashMapBackedNode().put("path", path).put("event_type", event_type));
    }
}
