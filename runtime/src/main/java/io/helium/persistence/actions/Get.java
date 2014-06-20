package io.helium.persistence.actions;

import io.helium.common.Path;
import io.helium.persistence.Persistence;
import io.helium.persistence.mapdb.Node;
import org.vertx.java.core.Future;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by Christoph Grotz on 15.06.14.
 */
public class Get extends CommonPersistenceVerticle{

    @Override
    public void start() {
        vertx.eventBus().registerHandler( Persistence.GET, this::handle );
    }

    public void handle(Message<JsonObject> event) {
        if (Node.exists(Path.of(event.body().getString("path")))) {
            event.reply(Node.of(Path.of(event.body().getString("path"))).toJsonObject());
        } else {
            Object value = get(Path.of(event.body().getString("path")));
            if (value instanceof Node) {
                event.reply(((Node) value).toJsonObject());
            } else {
                event.reply(value);
            }
        }
    }

    public static JsonObject request(Path path) {
        return new JsonObject().putString("path", path.toString());
    }
}
