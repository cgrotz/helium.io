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

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import io.helium.connectivity.HeliumServer;
import io.helium.connectivity.messaging.HeliumEndpoint;
import io.helium.disruptor.HeliumDisruptor;
import io.helium.disruptor.processor.distribution.Distributor;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.rulebased.RuleBasedAuthorization;
import io.helium.persistence.inmemory.InMemoryPersistence;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main entry point for Helium
 *
 * @author Christoph Grotz
 */
public class Helium {

    private static Options options = new Options();

    static {
        @SuppressWarnings("static-access")
        Option directoryOption = OptionBuilder.withArgName("directory").hasArg()
                .withDescription("Journal directory").create("d");

        @SuppressWarnings("static-access")
        Option basePathOption = OptionBuilder.withArgName("basepath").hasArg()
                .withDescription("basePath of the Helium instance").create("b");

        @SuppressWarnings("static-access")
        Option productiveModeOption = OptionBuilder.withArgName("productionMode").hasArg()
                .withDescription("set to true if the application runs in productive mode").create("prod");

        options.addOption(directoryOption);
        options.addOption(basePathOption);
        options.addOption(productiveModeOption);

        options.addOption("p", true, "Port for the webserver");
    }

    private InMemoryPersistence persistence;
    private HeliumDisruptor disruptor;
    private RuleBasedAuthorization authorization;
    private Set<HeliumEndpoint> endpoints = Sets.newHashSet();

    public Helium(String basePath, Node rule, File journalDirectory) throws IOException {
        checkNotNull(basePath);
        checkNotNull(journalDirectory);
        this.authorization = new RuleBasedAuthorization(rule);
        this.persistence = new InMemoryPersistence(this.authorization, this);

        this.disruptor = new HeliumDisruptor(journalDirectory, this.persistence,
                this.authorization);
    }

    public Helium(String basePath, File journalDirectory) throws IOException {
        checkNotNull(basePath);
        checkNotNull(journalDirectory);
        this.authorization = new RuleBasedAuthorization(Authorization.ALL_ACCESS_RULE);
        this.persistence = new InMemoryPersistence(this.authorization, this);

        this.disruptor = new HeliumDisruptor(journalDirectory, this.persistence,
                this.authorization);
    }

    public Helium(String basePath) throws IOException {
        checkNotNull(basePath);
        this.authorization = new RuleBasedAuthorization(Authorization.ALL_ACCESS_RULE);
        this.persistence = new InMemoryPersistence(this.authorization, this);

        this.disruptor = new HeliumDisruptor(File.createTempFile("Temp" + System.currentTimeMillis(), ""), this.persistence,
                this.authorization);
    }

    public static void main(String[] args) {
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String directory = cmd.getOptionValue("d");
            String basePath = cmd.getOptionValue("b", "http://localhost:8080");
            String host = cmd.getOptionValue("h", "localhost");
            int port = Integer.parseInt(cmd.getOptionValue("p", "8080"));
            boolean productiveMode = Boolean.parseBoolean(cmd.getOptionValue("prod", "false"));

            HeliumServer server = new HeliumServer(basePath, productiveMode, host, port, directory);
            server.run();
        } catch (ParseException e) {
            System.out.println(e.getLocalizedMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("helium", options);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
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
}
