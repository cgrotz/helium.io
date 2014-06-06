package io.helium.server.protocols.mqtt;

import io.helium.authorization.Authorization;
import io.helium.persistence.Persistence;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class MqttServer implements Runnable {
    private final Persistence persistence;
    private final Authorization authorization;
    private final int port;

    public MqttServer(int port, Persistence persistence, Authorization authorization) {
        this.port = port;
        this.persistence = persistence;
        this.authorization = authorization;
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new MqttServerInitializer(persistence, authorization, core));

            Channel ch = b.bind(port).sync().channel();
            System.out.println("Connect via MQTT to mqtt://localhost/");

            ch.closeFuture().sync();
        } catch (InterruptedException e) {

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
