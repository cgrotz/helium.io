package io.helium.server.protocols.mqtt;

import io.helium.core.Core;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by balu on 25.05.14.
 */
public class MqttServerHandler extends ChannelHandlerAdapter {
    private final Core core;
    private final Persistence persistence;
    private final Authorization authorization;

    public MqttServerHandler(Persistence persistence, Authorization authorization, Core core) {
        this.persistence = persistence;
        this.authorization = authorization;
        this.core = core;
    }


    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception { // (2)
        System.out.println((ByteBuf) msg);
        // Discard the received data silently.
        ((ByteBuf) msg).release(); // (3)
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
