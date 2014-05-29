package io.helium.server.protocols.mqtt;

import io.helium.core.Core;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;

/**
 * Created by balu on 25.05.14.
 */
public class MqttServerInitializer extends ChannelInitializer<SocketChannel> {
    private final Core core;
    private final Persistence persistence;
    private final Authorization authorization;
    private final DB db = DBMaker.newFileDB(new File("mqttEndpoints"))
            .closeOnJvmShutdown()
            .make();

    public MqttServerInitializer(Persistence persistence, Authorization authorization, Core core) {
        this.core = core;
        this.persistence = persistence;
        this.authorization = authorization;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("handler", new MqttServerHandler(persistence, authorization, core, db));
    }
}