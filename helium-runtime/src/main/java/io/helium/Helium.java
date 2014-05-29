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

import io.helium.core.Core;
import io.helium.json.Node;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.rule.RuleBasedAuthorization;
import io.helium.persistence.inmemory.InMemoryPersistence;
import io.helium.server.protocols.http.HttpServer;
import io.helium.server.protocols.mqtt.MqttServer;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final HttpServer httpServer;
    private final MqttServer mqttServer;

    private InMemoryPersistence persistence;
    private Core core;
    private RuleBasedAuthorization authorization;

    private ExecutorService executor = Executors.newCachedThreadPool();

    public Helium(String basePath, Node rule, File journalDirectory, String host, int port) throws IOException {
        checkNotNull(basePath);
        checkNotNull(journalDirectory);
        authorization = new RuleBasedAuthorization(rule);
        persistence = new InMemoryPersistence(authorization);
        core = new Core(journalDirectory, persistence, authorization);
        persistence.setCore(core);
        httpServer = new HttpServer(port, basePath, host, core, persistence, authorization);
        mqttServer = new MqttServer(core, persistence, authorization);
    }

    public Helium(String basePath, File journalDirectory, String host, int port) throws IOException {
        checkNotNull(basePath);
        checkNotNull(journalDirectory);
        authorization = new RuleBasedAuthorization(Authorization.ALL_ACCESS_RULE);
        persistence = new InMemoryPersistence(authorization);
        core = new Core(journalDirectory, persistence, authorization);
        persistence.setCore(core);
        httpServer = new HttpServer(port, basePath, host, core, persistence, authorization);
        mqttServer = new MqttServer(core, persistence, authorization);
    }

    public Helium(String basePath, String host, int port) throws IOException {
        checkNotNull(basePath);
        authorization = new RuleBasedAuthorization(Authorization.ALL_ACCESS_RULE);
        persistence = new InMemoryPersistence(authorization);

        core = new Core(File.createTempFile("Temp" + System.currentTimeMillis(), ""),
                persistence,
                authorization);
        persistence.setCore(core);
        httpServer = new HttpServer(port, basePath, host, core, persistence, authorization);
        mqttServer = new MqttServer(core, persistence, authorization);
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
            if (productiveMode) {
                Helium helium = new Helium(basePath, new File(directory), host, port);
                helium.start();
            } else {
                Helium helium = new Helium(basePath, createTempDirectory(), host, port);
                helium.start();
            }

        } catch (ParseException e) {
            System.out.println(e.getLocalizedMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("helium", options);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    private void start() throws InterruptedException {
        executor.submit(httpServer);
        executor.submit(mqttServer);
    }

    public static File createTempDirectory() throws IOException {
        final File temp;
        temp = File.createTempFile("Temp" + System.currentTimeMillis(), "");
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }
}
