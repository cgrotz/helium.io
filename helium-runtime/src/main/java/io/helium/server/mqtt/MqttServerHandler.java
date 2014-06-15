package io.helium.server.mqtt;

import io.helium.server.mqtt.decoder.MqttDecoder;
import io.helium.server.mqtt.encoder.Encoder;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Container;

import java.io.File;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class MqttServerHandler implements Handler<NetSocket> {
    private final DB db;

    private final MqttDecoder decoder = new MqttDecoder();
    private final Encoder encoder = new Encoder();
    private final Vertx vertx;
    private final Container container;

    public MqttServerHandler(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;
        File file = new File(container.config().getString("directory"));
        file.getParentFile().mkdirs();
        this.db = DBMaker.newFileDB(file)
                .asyncWriteEnable()
                .asyncWriteQueueSize(10)
                .closeOnJvmShutdown()
                .make();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if(!db.isClosed()) {
                        System.out.println("Shutdown MapDB Mqtt Endpoint Store");
                        db.commit();
                        db.compact();
                        db.close();
                    }
                }
                catch( Exception e) {
                }
            }
        });
    }

    @Override
    public void handle(NetSocket socket) {
        MqttEndpoint endpoint = new MqttEndpoint(socket, vertx, db);
    }
}
