package io.helium.server.channels.mqtt;

import com.google.common.collect.Maps;
import io.helium.server.channels.mqtt.decoder.MqttDecoder;
import io.helium.server.channels.mqtt.encoder.Encoder;
import io.helium.server.distributor.Endpoints;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Container;

import java.io.File;
import java.util.Map;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class MqttServerHandler implements Handler<NetSocket> {
    private final DB db = DBMaker.newFileDB(new File("helium/mqttEndpoints"))
            .transactionDisable()
            .closeOnJvmShutdown()
            .make();

    private final MqttDecoder decoder = new MqttDecoder();
    private final Encoder encoder = new Encoder();
    private final Vertx vertx;
    private final Container container;
    private Map<NetSocket, MqttEndpoint> endpoints = Maps.newHashMap();

    public MqttServerHandler(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.close();
            }
        });
    }

    @Override
    public void handle(NetSocket socket) {
        MqttEndpoint endpoint = new MqttEndpoint(socket, vertx, db);
        endpoints.put(socket, endpoint);
        Endpoints.get().addEndpoint(endpoint);
        socket.closeHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                MqttEndpoint endpoint = endpoints.remove(socket);
                Endpoints.get().removeEndpoint(endpoint);
            }
        });
    }
}
