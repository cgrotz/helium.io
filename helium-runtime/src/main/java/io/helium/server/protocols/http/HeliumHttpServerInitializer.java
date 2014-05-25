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

package io.helium.server.protocols.http;

import io.helium.core.Core;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Created by balu on 18.05.14.
 */
public class HeliumHttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Core core;
    private final Persistence persistence;
    private final Authorization authorization;
    private String basePath;

    public HeliumHttpServerInitializer(String basePath, Persistence persistence, Authorization authorization, Core core) {
        this.persistence = persistence;
        this.authorization = authorization;
        this.core = core;
        this.basePath = basePath;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("codec-http", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("handler", new HeliumHttpServerHandler(basePath, persistence, authorization, core));
    }
}
