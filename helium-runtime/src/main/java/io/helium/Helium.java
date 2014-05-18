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

package io.helium;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import io.helium.authorization.Authorization;
import io.helium.authorization.rulebased.RuleBasedAuthorization;
import io.helium.disruptor.HeliumDisruptor;
import io.helium.disruptor.processor.distribution.Distributor;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.messaging.HeliumEndpoint;
import io.helium.persistence.Persistence;
import io.helium.persistence.inmemory.InMemoryPersistence;
import io.netty.channel.Channel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main entry point for Helium
 *
 * @author Christoph Grotz
 */
public class Helium {

    private InMemoryPersistence persistence;

    private HeliumDisruptor disruptor;

    private RuleBasedAuthorization authorization;

    private Set<HeliumEndpoint> endpoints = Sets.newHashSet();

    public Helium(String basePath, Node rule, File journalDirectory,
                  Optional<File> snapshotDirectory) throws IOException {
        checkNotNull(basePath);
        checkNotNull(journalDirectory);
        this.authorization = new RuleBasedAuthorization(rule);
        this.persistence = new InMemoryPersistence(this.authorization, this);

        this.disruptor = new HeliumDisruptor(journalDirectory, snapshotDirectory, this.persistence,
                this.authorization);
    }

    public Helium(String basePath, File journalDirectory, Optional<File> snapshotDirectory)
            throws IOException {
        this(checkNotNull(basePath), Authorization.ALL_ACCESS_RULE, checkNotNull(journalDirectory),
                snapshotDirectory);
    }

    public Helium(String basePath) throws IOException {
        this(checkNotNull(basePath), Authorization.ALL_ACCESS_RULE, createTempDirectory().get(),
                createTempDirectory());
    }

    public static String loadJsFile() throws IOException {
        URL uuid = Thread.currentThread().getContextClassLoader().getResource("uuid.js");
        URL rpc = Thread.currentThread().getContextClassLoader().getResource("rpc.js");
        URL reconnectingwebsocket = Thread.currentThread().getContextClassLoader()
                .getResource("reconnecting-websocket.min.js");
        URL helium = Thread.currentThread().getContextClassLoader().getResource("helium.js");

        String uuidContent = com.google.common.io.Resources.toString(uuid, Charsets.UTF_8);
        String reconnectingWebsocketContent = com.google.common.io.Resources.toString(
                reconnectingwebsocket, Charsets.UTF_8);
        String rpcContent = com.google.common.io.Resources.toString(rpc, Charsets.UTF_8);
        String heliumContent = com.google.common.io.Resources.toString(helium, Charsets.UTF_8);

        return uuidContent + "\r\n" + reconnectingWebsocketContent + "\r\n" + rpcContent + "\r\n"
                + heliumContent;
    }

    private static Optional<File> createTempDirectory() throws IOException {
        final File temp;
        temp = File.createTempFile("Temp" + System.currentTimeMillis(), "");
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return Optional.fromNullable(temp);
    }

    public void handle(HeliumEvent heliumEvent) {
        heliumEvent.setFromHistory(false);
        this.disruptor.handleEvent(heliumEvent);
    }

    public void handleEvent(HeliumEventType type, String nodePath, Optional<?> value) {
        HeliumEvent heliumEvent = new HeliumEvent(type, nodePath, value);
        handle(heliumEvent);
    }

    public void distributeChangeLog(ChangeLog changeLog) {
        for (HeliumEndpoint endpoint : this.endpoints) {
            endpoint.distributeChangeLog(changeLog);
        }
    }

    public void addEndpoint(HeliumEndpoint endpoint) {
        this.disruptor.addEndpoint(endpoint);
        this.endpoints.add(endpoint);
    }

    public void removeEndpoint(HeliumEndpoint endpoint) {
        this.disruptor.removeEndpoint(endpoint);
        this.endpoints.remove(endpoint);
    }

    public Distributor getDistributor() {
        return this.disruptor.getDistributor();
    }

    public Persistence getPersistence() {
        return this.persistence;
    }

    public Authorization getAuthorization() {
        return this.authorization;
    }

    public void closeChannel(Channel channel) {

    }
}
