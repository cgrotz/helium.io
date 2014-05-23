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

package io.helium.web;

import io.helium.Helium;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.File;
import java.io.IOException;

public class HeliumServer {

    private final int port;
    private Helium helium;
    private String basePath;
    private String host;

    public HeliumServer(String basePath, boolean productiveMode, String host, int port,
                        String journalDir) throws IOException {
        this.port = port;
        this.basePath = basePath;
        this.host = host;
        if (productiveMode) {
            this.helium = new Helium(basePath, new File(journalDir));
        } else {
            this.helium = new Helium(basePath, createTempDirectory());
        }
    }

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HeliumServerInitializer(basePath, helium));

            Channel ch = b.bind(host, port).sync().channel();
            System.out.println("Helium server started at port " + port + '.');
            System.out.println("Open your browser and navigate to http://localhost:" + port + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
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