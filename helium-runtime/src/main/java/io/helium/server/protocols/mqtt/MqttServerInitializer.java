package io.helium.server.protocols.mqtt;

import io.helium.core.Core;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * Created by balu on 25.05.14.
 */
public class MqttServerInitializer extends ChannelInitializer<SocketChannel> {
    private final Core core;
    private final Persistence persistence;
    private final Authorization authorization;

    public MqttServerInitializer(Persistence persistence, Authorization authorization, Core core) {
        this.core = core;
        this.persistence = persistence;
        this.authorization = authorization;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("handler", new MqttServerHandler(persistence, authorization, core));
    }
}
