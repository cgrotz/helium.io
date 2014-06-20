package io.helium.management;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

/**
 * Created by Christoph Grotz on 20.06.14.
 */
public class ManagementConsole extends Verticle {
    @Override
    public void start() {
        RouteMatcher matcher = new RouteMatcher();

        matcher.noMatch(new UiHandler());
        vertx.createHttpServer().requestHandler(matcher).listen(8081);
    }
}
