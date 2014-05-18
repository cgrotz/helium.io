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

import com.google.common.collect.Maps;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class HeliumConnection {

    private static HeliumConnection instance = new HeliumConnection();
    private Map<String, HttpClient> clients = Maps.newHashMap();

    private HeliumConnection() {

    }

    public static HeliumConnection getInstance() {
        return instance;
    }

    public void getSocketFor(String path, Handler<WebSocket> handler) throws MalformedURLException {
        URL url = new URL(path);
        HttpClient client;
        if (clients.containsKey(url.getHost() + ":" + url.getPort())) {
            client = clients.get(url.getHost() + ":" + url.getPort());
        } else {
            client = VertxFactory.newVertx().createHttpClient().setHost(url.getHost())
                    .setPort(url.getPort());
        }
        client.connectWebsocket(url.getPath(), handler);
    }
}
