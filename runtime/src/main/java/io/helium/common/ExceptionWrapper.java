package io.helium.common;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

/**
 * Helper for handling eventbus exception chains
 *
 * Created by Christoph Grotz on 06.07.14.
 */
public class ExceptionWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionWrapper.class);
    public static <T> Handler<Message<T>> wrap(Handler<Message<T>> handler) {
        return event -> {
            try {
                handler.handle(event);
            }
            catch(Exception e) {
                LOGGER.error("Error handling event {}", event, e);
                event.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
            }
        };
    }
}
