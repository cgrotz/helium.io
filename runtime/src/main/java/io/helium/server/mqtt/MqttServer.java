package io.helium.server.mqtt;

import org.vertx.java.core.Future;
import org.vertx.java.platform.Verticle;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class MqttServer extends Verticle {

    @Override
    public void start(Future<Void> startedResult) {
        try {
            vertx.createNetServer().connectHandler(new MqttServerHandler(vertx, container)).listen(container.config().getInteger("port",1883));
            startedResult.complete();
        }
        catch(Exception e) {
            startedResult.setFailure(e);
        }
    }
}
