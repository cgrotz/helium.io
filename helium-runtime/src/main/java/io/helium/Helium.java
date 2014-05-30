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

import com.google.common.base.Strings;
import com.google.common.io.Files;
import io.helium.common.Path;
import io.helium.core.Core;
import io.helium.json.Node;
import io.helium.persistence.authorization.chained.ChainedAuthorization;
import io.helium.persistence.authorization.rule.RuleBasedAuthorization;
import io.helium.persistence.inmemory.InMemoryPersistence;
import io.helium.server.protocols.http.HttpServer;
import io.helium.server.protocols.mqtt.MqttServer;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
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

        options.addOption(directoryOption);
        options.addOption(basePathOption);

        options.addOption("p", true, "Port for the webserver");
    }

    private final HttpServer httpServer;
    private final MqttServer mqttServer;

    private InMemoryPersistence persistence;
    private Core core;
    private ChainedAuthorization authorization;

    private ExecutorService executor = Executors.newCachedThreadPool();

    public Helium(String basePath, File journalDirectory, String host, int httpPort, int mqttPort) throws IOException {
        checkNotNull(basePath);
        checkNotNull(journalDirectory);
        persistence = new InMemoryPersistence();
        authorization = new ChainedAuthorization(new RuleBasedAuthorization(persistence));
        persistence.setAuthorization(authorization);
        core = new Core(journalDirectory, persistence, authorization);
        persistence.setCore(core);
        httpServer = new HttpServer(httpPort, basePath, host, core, persistence, authorization);
        mqttServer = new MqttServer(mqttPort, core, persistence, authorization);

        initDefaults();
    }

    public Helium(String basePath, String host, int httpPort, int mqttPort) throws IOException {
        checkNotNull(basePath);
        persistence = new InMemoryPersistence();
        authorization = new ChainedAuthorization(new RuleBasedAuthorization(persistence));
        persistence.setAuthorization(authorization);

        core = new Core(File.createTempFile("Temp" + System.currentTimeMillis(), ""),
                persistence,
                authorization);
        persistence.setCore(core);
        httpServer = new HttpServer(httpPort, basePath, host, core, persistence, authorization);
        mqttServer = new MqttServer(mqttPort, core, persistence, authorization);

        initDefaults();
    }

    private void initDefaults() {

        if(!persistence.exists(Path.of("/users"))) {
            String uuid = UUID.randomUUID().toString();
            persistence.getNode(Path.of("/users")).put(uuid.replaceAll("-",""),
                    new Node().put("username","admin").put("password", "admin").put("isAdmin", true)
                            .put("permissions", new Node()));
        }

        if(!persistence.exists(Path.of("/rules"))) {
            Node rules = persistence.getNode(Path.of("/rules"));
            rules.put(".write", "function(auth, path, data, root){\n" +
                    "   if(auth.isAdmin) {\n" +
                    "       return true;\n" +
                    "   }\n" +
                    "   else {\n" +
                    "       return false;\n" +
                    "   }\n" +
                    "}\n");
            rules.put(".read", true);
            rules.put("rules", new Node().put(".write", "function(auth, path, data, root){\n" +
                    "   if(auth.isAdmin) {\n" +
                    "       return true;\n" +
                    "   }\n" +
                    "   else {\n" +
                    "       return false;\n" +
                    "   }\n" +
                    "}\n")
                    .put(".read", "function(auth, path, data, root){\n" +
                            "   if(auth.isAdmin) {\n" +
                            "       return true;\n" +
                            "   }\n" +
                            "   else {\n" +
                            "       return false;\n" +
                            "   }\n" +
                            "}\n"));
            rules.put("users", new Node().put(".write", "function(auth, path, data, root){\n" +
                    "   if(auth.isAdmin) {\n" +
                    "       return true;\n" +
                    "   }\n" +
                    "   else {\n" +
                    "       return false;\n" +
                    "   }\n" +
                    "}\n")
                    .put(".read", "function(auth, path, data, root){\n" +
                            "   if(auth.isAdmin) {\n" +
                            "       return true;\n" +
                            "   }\n" +
                            "   else {\n" +
                            "       return false;\n" +
                            "   }\n" +
                            "}\n"));
        }
    }

    public static void main(String[] args) {
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String directory = cmd.getOptionValue("d");
            String basePath = cmd.getOptionValue("b", "http://localhost:8080");
            String host = cmd.getOptionValue("h", "localhost");
            int httpPort = Integer.parseInt(cmd.getOptionValue("p", "8080"));
            int mqttPort = Integer.parseInt(cmd.getOptionValue("m", "1883"));
            File file = new File(Strings.isNullOrEmpty(directory)?"helium":directory);
            Files.createParentDirs(new File(file,".helium"));

            Helium helium = new Helium( basePath, file, host, httpPort, mqttPort );
            helium.start();
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
}
