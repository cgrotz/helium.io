package io.helium.server.protocols.mqtt;

import io.helium.core.Core;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.server.protocols.mqtt.decoder.MqttDecoder;
import io.helium.server.protocols.mqtt.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.util.Optional;

/**
 * Created by balu on 25.05.14.
 */
public class MqttServerHandler extends ChannelHandlerAdapter {
    private final Core core;
    private final Persistence persistence;
    private final Authorization authorization;
    private final MqttDecoder decoder;

    public MqttServerHandler(Persistence persistence, Authorization authorization, Core core) {
        this.persistence = persistence;
        this.authorization = authorization;
        this.core = core;
        this.decoder = new MqttDecoder();
    }


    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception { // (2)
        System.out.println((ByteBuf) msg);
        Optional<Command> command = decoder.decode((ByteBuf) msg);
        System.out.println(command);
        // Discard the received data silently.
        ((ByteBuf) msg).release(); // (3)
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
