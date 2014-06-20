package io.helium.server.http;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.helium.authorization.Authorizator;
import io.helium.authorization.Operation;
import io.helium.common.PasswordHelper;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.builder.HeliumEventBuilder;
import io.helium.persistence.EventSource;
import io.helium.persistence.Persistence;
import io.helium.common.DataTypeConverter;
import io.helium.persistence.actions.Get;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

/**
 * Created by Christoph Grotz on 29.05.14.
 */
public class RestHandler implements Handler<HttpServerRequest> {
    private final Vertx vertx;
    private final String basePath;

    public RestHandler(Vertx vertx, String basePath) {
        this.basePath = basePath;
        this.vertx = vertx;
    }

    @Override
    public void handle(HttpServerRequest req) {
        try {
            Path nodePath = new Path(HeliumEvent.extractPath(req.uri().replaceAll("\\.json", "")));
            if (req.uri().endsWith("helium.js")) {
                req.response().end(loadJsFile());
            } else if (req.method().equalsIgnoreCase(HttpMethod.GET.name())) {
                get(req, nodePath);
            } else if (req.method().equalsIgnoreCase(HttpMethod.POST.name())) {
                post(req);
            } else if (req.method().equalsIgnoreCase(HttpMethod.PUT.name())) {
                put(req);
            } else if (req.method().equalsIgnoreCase(HttpMethod.DELETE.name())) {
                delete(req);
            } else {
                req.response().setStatusCode(404).end();
            }
        } catch (Exception e) {
            req.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).setStatusMessage(e.getMessage()).end();
        }
    }

    private void delete(HttpServerRequest req) {
        Path nodePath = Path.of(req.uri());
        extractAuthentication(req, auth -> {
            HeliumEvent event = HeliumEventBuilder.delete(nodePath).withAuth(auth).build();
            if (auth.isPresent())
                event.setAuth(auth.get());

            vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, nodePath, null), (Message<Boolean> event1) -> {
                if (event1.body()) {
                    vertx.eventBus().send(Persistence.DELETE, event, new Handler<Message>() {
                        @Override
                        public void handle(Message event) {
                            req.response().end();
                        }
                    });
                    vertx.eventBus().send(EventSource.PERSIST_EVENT, event);
                }
            });
        });
        req.response().end();
    }

    private void put(HttpServerRequest req) {
        req.bodyHandler(buffer -> {
            Path nodePath = Path.of(req.uri());
            extractAuthentication(req, auth -> {
                Object data = DataTypeConverter.convert(buffer);
                HeliumEvent event = HeliumEventBuilder.set(nodePath, data).withAuth(auth).build();
                if (auth.isPresent())
                    event.setAuth(auth.get());

                vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, nodePath, data), (Message<Boolean> event1) -> {
                    if (event1.body()) {
                        vertx.eventBus().send(EventSource.PERSIST_EVENT, event);
                        vertx.eventBus().send(event.getType().eventBus, event);
                    }
                });
            });
            req.response().end();
        });
        req.resume();
    }

    private void post(HttpServerRequest req) {
        req.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(final Buffer buffer) {
                String uri;
                String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                if (req.uri().endsWith("/")) {
                    uri = req.uri() + uuid;
                } else {
                    uri = req.uri() + "/" + uuid;
                }
                Path nodePath = Path.of(uri);
                extractAuthentication(req, auth -> {
                    Object data = DataTypeConverter.convert(buffer);
                    HeliumEvent event = HeliumEventBuilder.set(nodePath, data).withAuth(auth).build();
                    if (auth.isPresent())
                        event.setAuth(auth.get());

                    vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.WRITE, auth, nodePath, data), (Message<Boolean> event1) -> {
                        if (event1.body()) {
                            vertx.eventBus().send(EventSource.PERSIST_EVENT, event);
                            vertx.eventBus().send(event.getType().eventBus, event);

                        }
                    });
                });
                req.response().end();
            }
        });
        req.resume();
    }

    private void get(HttpServerRequest req, Path path) {
        extractAuthentication(req, auth -> {
            vertx.eventBus().send(Persistence.GET, Get.request(path), (Message<Object> msg) -> {
                vertx.eventBus().send(Authorizator.CHECK, Authorizator.check(Operation.READ, auth, path, msg.body()), new Handler<Message<Boolean>>() {
                    @Override
                    public void handle(Message<Boolean> event) {
                        if (event.body()) {
                            vertx.eventBus().send(Authorizator.FILTER,
                                    Authorizator.filter(auth, path, msg.body()),
                                    new Handler<Message<Object>>() {
                                        @Override
                                        public void handle(Message<Object> event) {
                                            if (event.body() != null) {
                                                req.response().end(event.body().toString());
                                            } else {
                                                req.response().setStatusCode(404).end();
                                            }
                                        }
                                    }
                            );
                        } else {
                            req.response().setStatusCode(UNAUTHORIZED.code()).end();
                        }
                    }
                });
            });
        });
        req.resume();
    }

    public static String loadJsFile() throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL uuid = cl.getResource("js/uuid.js");
        URL rpc = cl.getResource("js/rpc.js");
        URL reconnectingWebSocket = cl.getResource("js/reconnecting-websocket.min.js");
        URL helium = cl.getResource("js/helium.js");

        String uuidContent = Resources.toString(uuid, Charsets.UTF_8);
        String reconnectingWebSocketContent = Resources.toString(reconnectingWebSocket, Charsets.UTF_8);
        String rpcContent = Resources.toString(rpc, Charsets.UTF_8);
        String heliumContent = Resources.toString(helium, Charsets.UTF_8);

        return uuidContent + "\r\n" + reconnectingWebSocketContent + "\r\n" + rpcContent + "\r\n"
                + heliumContent;
    }

    private void extractAuthentication(HttpServerRequest req, Handler<Optional<JsonObject>> handler) {
        Optional<JsonObject> auth = Optional.empty();
        if (req.headers().contains(HttpHeaders.Names.AUTHORIZATION)) {
            String authorizationToken = req.headers().get(HttpHeaders.Names.AUTHORIZATION);
            JsonObject authentication = Authorizator.decode(authorizationToken);
            String username = authentication.getString("username");
            String password = authentication.getString("password");

            vertx.eventBus().send(Persistence.GET,
                    Get.request(Path.of("/users")),
                    new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    JsonObject users = event.body();
                    for (String key : users.getFieldNames()) {
                        Object value = users.getObject(key);
                        if (value instanceof JsonObject) {
                            JsonObject node = (JsonObject) value;
                            if (node.containsField("username") && node.containsField("password")) {
                                String localUsername = node.getString("username");
                                String localPassword = node.getString("password");
                                if (username.equals(localUsername) &&
                                        PasswordHelper.get().comparePassword(localPassword,password)) {
                                    handler.handle(Optional.of(node));
                                    return;
                                }
                            }
                        }
                    }
                    handler.handle(auth);
                }
            });
        } else {
            handler.handle(auth);
        }
    }
}