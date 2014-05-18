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

package io.helium.runtime;

import com.google.common.base.Optional;
import io.helium.Helium;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

public class HeliumServer {

    private static Options options = new Options();
    private final int port;
    private Helium helium;
    private String basePath;
    private String host;

    static {
        @SuppressWarnings("static-access")
        Option directoryOption = OptionBuilder.withArgName("directory").hasArg()
                .withDescription("Journal directory").create("d");

        @SuppressWarnings("static-access")
        Option snaphshotOption = OptionBuilder.withArgName("snapshot").hasArg()
                .withDescription("Snapshot directory").create("s");

        @SuppressWarnings("static-access")
        Option basePathOption = OptionBuilder.withArgName("basepath").hasArg()
                .withDescription("basePath of the Helium instance").create("b");

        @SuppressWarnings("static-access")
        Option productiveModeOption = OptionBuilder.withArgName("productionMode").hasArg()
                .withDescription("set to true if the application runs in productive mode").create("prod");

        options.addOption(directoryOption);
        options.addOption(snaphshotOption);
        options.addOption(basePathOption);
        options.addOption(productiveModeOption);

        options.addOption("p", true, "Port for the webserver");
    }

    public HeliumServer(String basePath, boolean productiveMode, String host, int port,
                        String journalDir, String snapshotDir) throws IOException {
        this.port = port;
        this.basePath = basePath;
        this.host = host;
        if (productiveMode) {
            Optional<File> snapshotDirectory = Optional.of(new File(snapshotDir));
            this.helium = new Helium(basePath, new File(journalDir), snapshotDirectory);
        } else {
            Optional<File> snapshotDirectory = Optional.of(createTempDirectory());
            this.helium = new Helium(basePath, createTempDirectory(), snapshotDirectory);
        }
    }

    public static void main(String[] args) {
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String directory = cmd.getOptionValue("d");
            String basePath = cmd.getOptionValue("b", "http://localhost:8080");
            String host = cmd.getOptionValue("h", "localhost");
            String snapshotDirectory = cmd.getOptionValue("s");
            int port = Integer.parseInt(cmd.getOptionValue("p", "8080"));
            boolean productiveMode = Boolean.parseBoolean(cmd.getOptionValue("prod", "false"));
            new HeliumServer(basePath, productiveMode, host, port, directory, snapshotDirectory)
                    .run();
        } catch (ParseException e) {
            System.out.println(e.getLocalizedMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("helium", options);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }
/*
    private RouteMatcher createRoutMatcher(HeliumServerHandler rsh) {
		RouteMatcher rm = new RouteMatcher();
		rm.getWithRegEx("(.)*helium.js", rsh.getHeliumFileHttpHandler());
		rm.allWithRegEx("(.)*.json", rsh.getRestHttpHandler());
		rm.noMatch(rsh.getAdminHttpHandler());
		return rm;
	}*/

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HeliumServerInitializer(basePath, helium));

            Channel ch = b.bind(port).sync().channel();
            System.out.println("Helium server started at port " + port + '.');
            System.out.println("Open your browser and navigate to http://localhost:" + port + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }


        /*
		HeliumServerHandler rsh = new HeliumServerHandler(basePath, helium);
		Vertx vertx = VertxFactory.newVertx();
		HttpServer server = vertx.createHttpServer();
		server.requestHandler(createRoutMatcher(rsh));
		server.websocketHandler(rsh.getWebsocketHandler());
		server.listen(8080, host);*/
    }

    public File createTempDirectory() throws IOException {
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