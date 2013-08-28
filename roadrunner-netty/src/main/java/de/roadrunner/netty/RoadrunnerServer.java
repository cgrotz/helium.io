/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package de.roadrunner.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Optional;

import de.skiptag.roadrunner.Roadrunner;

/**
 * A HTTP server which serves Web Socket requests at:
 * 
 * http://localhost:8080/websocket
 * 
 * Open your browser at http://localhost:8080/, then the demo page will be
 * loaded and a Web Socket connection will be made automatically.
 * 
 * This server illustrates support for the different web socket specification
 * versions and will work with:
 * 
 * <ul>
 * <li>Safari 5+ (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 6-13 (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 14+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Chrome 16+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * <li>Firefox 7+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Firefox 11+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * </ul>
 */
public class RoadrunnerServer {

    private static Options options = new Options();
    private final int port;
    private Roadrunner roadrunner;

    static {
	@SuppressWarnings("static-access")
	Option directoryOption = OptionBuilder.withArgName("directory")
		.hasArg()
		.withDescription("Journal directory")
		.create("d");

	@SuppressWarnings("static-access")
	Option snaphshotOption = OptionBuilder.withArgName("snapshot")
		.hasArg()
		.withDescription("Snapshot directory")
		.create("s");

	@SuppressWarnings("static-access")
	Option basePathOption = OptionBuilder.withArgName("basepath")
		.hasArg()
		.withDescription("basePath of the Roadrunner instance")
		.isRequired()
		.create("b");

	@SuppressWarnings("static-access")
	Option productiveModeOption = OptionBuilder.withArgName("productionMode")
		.hasArg()
		.withDescription("set to true if the application runs in productive mode")
		.create("prod");

	options.addOption(directoryOption);
	options.addOption(snaphshotOption);
	options.addOption(basePathOption);
	options.addOption(productiveModeOption);

	options.addOption("p", true, "Port for the webserver");
    }

    public RoadrunnerServer(String basePath, boolean productiveMode, int port,
	    String journalDir, String snapshotDir) throws IOException {
	this.port = port;
	if (productiveMode) {
	    Optional<File> snapshotDirectory = Optional.of(new File(snapshotDir));
	    this.roadrunner = new Roadrunner(basePath, new File(journalDir),
		    snapshotDirectory);
	} else {
	    Optional<File> snapshotDirectory = Optional.of(createTempDirectory());
	    this.roadrunner = new Roadrunner(basePath, createTempDirectory(),
		    snapshotDirectory);
	}
    }

    public void run() throws InterruptedException {
	EventLoopGroup bossGroup = new NioEventLoopGroup();
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	try {
	    ServerBootstrap b = new ServerBootstrap();
	    b.group(bossGroup, workerGroup)
		    .channel(NioServerSocketChannel.class)
		    .childHandler(new WebSocketServerInitializer(roadrunner));

	    Channel ch = b.bind(port).sync().channel();
	    ch.closeFuture().sync();
	} finally {
	    bossGroup.shutdownGracefully();
	    workerGroup.shutdownGracefully();
	}
    }

    public static void main(String[] args) {
	CommandLineParser parser = new BasicParser();
	try {
	    CommandLine cmd = parser.parse(options, args);
	    String directory = cmd.getOptionValue("d");
	    String basePath = cmd.getOptionValue("b");
	    String snapshotDirectory = cmd.getOptionValue("s");
	    int port = Integer.parseInt(cmd.getOptionValue("p", "8080"));
	    boolean productiveMode = Boolean.parseBoolean(cmd.getOptionValue("prod", "false"));
	    new RoadrunnerServer(basePath, productiveMode, port, directory,
		    snapshotDirectory).run();
	} catch (ParseException e) {
	    System.out.println(e.getLocalizedMessage());
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.printHelp("roadrunner", options);
	} catch (Exception exp) {
	    exp.printStackTrace();
	}
    }

    public File createTempDirectory() throws IOException {
	final File temp;
	temp = File.createTempFile("Temp" + System.currentTimeMillis(), "");
	if (!(temp.delete())) {
	    throw new IOException("Could not delete temp file: "
		    + temp.getAbsolutePath());
	}

	if (!(temp.mkdir())) {
	    throw new IOException("Could not create temp directory: "
		    + temp.getAbsolutePath());
	}
	return temp;
    }
}