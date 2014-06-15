package io.helium.server.mqtt;

import org.vertx.java.platform.Verticle;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class MqttServer extends Verticle {

    @Override
    public void start() {
        vertx.createNetServer().connectHandler(new MqttServerHandler(vertx, container)).listen(1883);
        System.out.println("Connect via MQTT to mqtt://localhost/");
    }
}
