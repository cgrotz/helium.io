package io.helium.management;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.IOException;
import java.net.URL;

/**
 * Created by Christoph Grotz on 20.06.14.
 */
public class UiHandler implements Handler<HttpServerRequest> {
    @Override
    public void handle(HttpServerRequest request) {
        String uri = "management/build"+request.uri();
        if(Strings.isNullOrEmpty(request.uri()) || request.uri().equals("/")) {
            uri = uri+"index.html";
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource(uri);
        if(resource != null) {
            try {
                String content = Resources.toString(resource, Charsets.UTF_8);
                request.response().end(content);
            } catch (IOException e) {
                request.response()
                    .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                    .setStatusMessage(HttpResponseStatus.NOT_FOUND.reasonPhrase())
                    .end();
            }
        }
        else {
            request.response()
                    .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                    .setStatusMessage(HttpResponseStatus.NOT_FOUND.reasonPhrase())
                    .end();
        }
    }
}
