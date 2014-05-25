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

package io.helium.server;

import io.helium.core.Core;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.server.protocols.http.HeliumHttpServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;

public class HeliumServer {

    private final int port;
    private final Authorization authorization;
    private final Persistence persistence;
    private final Core core;
    private final String basePath;
    private final String host;

    public HeliumServer(int port, String basePath, String host, Core core, Persistence persistence, Authorization authorization) throws IOException {
        this.port = port;
        this.basePath = basePath;
        this.host = host;
        this.core = core;
        this.persistence = persistence;
        this.authorization = authorization;
    }

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HeliumHttpServerInitializer(basePath, persistence, authorization, core));

            Channel ch = b.bind(host, port).sync().channel();
            System.out.println("Helium server started");
            System.out.println("Open your browser and navigate to http://localhost:" + port + '/');
            //System.out.println("Connect via MQTT to mqtt://localhost/");
            //System.out.println("Connect via CoAP to coap://localhost/");

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}