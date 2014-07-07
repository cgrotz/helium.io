package io.helium.persistence.actions;

import io.helium.common.Path;
import io.helium.persistence.mapdb.MapDbService;
import io.helium.persistence.mapdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Abstraction for Getting Data from the Persistence Layer
 *
 * Created by Christoph Grotz on 15.06.14.
 */
public class Get extends CommonPersistenceVerticle{
    private static final Logger LOGGER = LoggerFactory.getLogger(Get.class);

    public void handle(Message<JsonObject> event) {
        long start = System.currentTimeMillis();
        if (MapDbService.get().exists(Path.of(event.body().getString("path")))) {
            event.reply(MapDbService.get().of(Path.of(event.body().getString("path"))).toJsonObject());
            LOGGER.info("Get Action took: "+(System.currentTimeMillis()-start)+"ms");
        } else {
            Object value = get(Path.of(event.body().getString("path")));
            if (value instanceof Node) {
                event.reply(((Node) value).toJsonObject());
                LOGGER.info("Get Action took: "+(System.currentTimeMillis()-start)+"ms");
            } else {
                event.reply(value);
                LOGGER.info("Get Action took: "+(System.currentTimeMillis()-start)+"ms");
            }
        }
    }

    public static JsonObject request(Path path) {
        return new JsonObject().putString("path", path.toString());
    }
}
