package io.helium.server.channels.mqtt;

import com.google.common.collect.Lists;
import io.helium.authorization.Authorizator;
import io.helium.authorization.Operation;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChildAddedLogEvent;
import io.helium.event.changelog.ValueChangedLogEvent;
import io.helium.persistence.Persistor;
import io.helium.server.DataTypeConverter;
import io.helium.server.Endpoint;
import io.helium.server.channels.mqtt.decoder.MqttDecoder;
import io.helium.server.channels.mqtt.encoder.Encoder;
import io.helium.server.channels.mqtt.protocol.*;
import io.helium.server.distributor.Distributor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.mapdb.DB;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetSocket;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Christoph Grotz on 26.05.14.
 */
public class MqttEndpoint implements Endpoint, Handler<Buffer> {
    private Optional<String> clientId = Optional.empty();
    private final NetSocket socket;
    private final DB db;

    private Set<Topic> topics;
    private final Encoder encoder = new Encoder();
    private final MqttDecoder decoder = new MqttDecoder();
    private final PathMatcher pathMatcher = new PathMatcher();
    private Optional<JsonObject> auth = Optional.empty();
    private Vertx vertx;


    public MqttEndpoint(NetSocket socket, Vertx vertx, DB db) {
        socket.dataHandler(this);
        this.socket = socket;
        this.vertx = vertx;
        this.db = db;
        this.topics = db.getHashSet(clientId + "Topics");
    }

    @Override
    public void distribute(HeliumEvent event) {
        distributeChangeLog(event.getChangeLog());
    }

    @Override
    public void distributeChangeLog(ChangeLog changeLog) {
        changeLog.forEach(obj -> {
            JsonObject logE = (JsonObject) obj;

            if (logE.getString("type").equals(ChildAddedLogEvent.class.getSimpleName())) {
                ChildAddedLogEvent logEvent = ChildAddedLogEvent.of(logE);
                if (hasListener(logEvent.getPath().append(logEvent.getName()), CHILD_ADDED)) {
                    fireChildAdded(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren()
                    );
                }
            }
            if (logE.getString("type").equals(ValueChangedLogEvent.class.getSimpleName())) {
                ValueChangedLogEvent logEvent = ValueChangedLogEvent.of(logE);
                if (hasListener(logEvent.getPath(), VALUE)) {
                    fireValue(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue());
                }
            }
        });
    }

    @Override
    public void fireChildAdded(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren) {
        try {
            vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, value),
                    new Handler<Message<Boolean>>() {
                        @Override
                        public void handle(Message<Boolean> event) {
                            if (event.body()) {
                                vertx.eventBus().send(Authorizator.FILTER_CONTENT,
                                        Authorizator.filter(auth, path, value),
                                        new Handler<Message<Object>>() {
                                            @Override
                                            public void handle(Message<Object> event) {
                                                if (event.body() != null) {
                                                    try {
                                                        ByteBuf buffer = Unpooled.buffer(1);
                                                        encoder.encodePublish(
                                                                buffer,
                                                                (int) System.currentTimeMillis(), path.append(name).toString(), event.body().toString());
                                                        socket.write(new Buffer(buffer));
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        }
                                );
                            }
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fireValue(String name, Path path, Path parent, Object value) {
        try {
            vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, value),
                    new Handler<Message<Boolean>>() {
                        @Override
                        public void handle(Message<Boolean> event) {
                            if (event.body()) {
                                vertx.eventBus().send(Authorizator.FILTER_CONTENT,
                                        Authorizator.filter(auth, path, value),
                                        new Handler<Message<Object>>() {
                                            @Override
                                            public void handle(Message<Object> event) {
                                                if (event.body() != null) {
                                                    try {
                                                        ByteBuf buffer = Unpooled.buffer();
                                                        encoder.encodePublish(buffer,
                                                                (int) System.currentTimeMillis(), path.append(name).toString(), event.body().toString());
                                                        socket.write(new Buffer(buffer));
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        }
                                );

                            }
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasListener(Path path, String type) {
        return matchesSubscribedTopics(path);
    }

    @Override
    public void distributeEvent(Path path, JsonObject payload) {
        if (matchesSubscribedTopics(path)) {
            try {
                vertx.eventBus().send(Authorizator.IS_AUTHORIZED, Authorizator.check(Operation.READ, auth, path, payload),
                        new Handler<Message<Boolean>>() {
                            @Override
                            public void handle(Message<Boolean> event) {
                                if (event.body()) {
                                    try {
                                        ByteBuf buffer = Unpooled.buffer(1);
                                        encoder.encodePublish(
                                                buffer,
                                                (int) System.currentTimeMillis(), path.toString(), payload.toString());
                                        socket.write(new Buffer(buffer));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void subscribeToTopics(List<Topic> topics) {
        this.topics.addAll(topics);
    }

    public void unsubscribeToTopics(List<Topic> topics) {
        this.topics.removeAll(topics);
    }

    public boolean matchesSubscribedTopics(Path path) {
        for (Topic topic : topics) {
            if (pathMatcher.matchPath(topic.getPattern(), path)) {
                return true;
            }
        }
        return false;
    }

    public Optional<JsonObject> getAuth() {
        return auth;
    }

    public void setAuth(JsonObject auth) {
        this.auth = Optional.ofNullable(auth);
    }

    @Override
    public void handle(Buffer event) {
        Optional<Command> command = null;
        try {
            command = decoder.decode(event);
            ByteBuf msg = event.getByteBuf();
            // Discard the received data silently.
            ((ByteBuf) msg).release(); // (3)
            if (command.isPresent()) {
                switch (command.get().getCommandType()) {
                    case CONNECT: {
                        Connect connect = (Connect) command.get();
                        String clientId = connect.getClientId();
                        this.clientId = Optional.ofNullable(clientId);
                        extractAuthentication(connect, new Handler<Optional<JsonObject>>() {
                            @Override
                            public void handle(Optional<JsonObject> event) {
                                //if (event.isPresent()) {
                                    auth = event;
                                    socket.write(new Buffer(encoder.encodeConnack(ConnackCode.Accepted)));
                                /*} else {
                                    socket.write(new Buffer(encoder.encodeConnack(ConnackCode.NotAuthorized)));
                                }*/
                            }
                        });
                        break;
                    }
                    case SUBSCRIBE: {
                        Subscribe subscribe = (Subscribe) command.get();

                        List<QosLevel> grantedQos = Lists.transform(subscribe.getTopics(), topic -> topic.getQosLevel());

                        subscribeToTopics(subscribe.getTopics());
                        // TODO CG Subscriptions need to be made persistent
                        socket.write(new Buffer(encoder.encodeSuback(subscribe.getMessageId(), grantedQos)));
                        break;
                    }
                    case UNSUBSCRIBE: {
                        Unsubscribe unsubscribe = (Unsubscribe) command.get();
                        unsubscribeToTopics(unsubscribe.getTopics());
                        socket.write(new Buffer(encoder.encodeUnsuback(unsubscribe.getMessageId())));
                        break;
                    }
                    case PINGREQ: {
                        socket.write(new Buffer(encoder.encodePingresp()));
                        break;
                    }
                    case DISCONNECT: {
                        socket.close();
                        break;
                    }
                    case PUBLISH: {
                        Publish publish = (Publish) command.get();
                        Object optional = DataTypeConverter.convert(publish.getArray());
                        if (publish.isRetainFlag()) {
                            HeliumEvent heliumEvent = new HeliumEvent(HeliumEventType.SET, publish.getTopic(), optional);
                            if (auth.isPresent())
                                heliumEvent.setAuth(auth.get());
                            vertx.eventBus().send(Authorizator.SUBSCRIPTION, heliumEvent);
                        } else {
                            vertx.eventBus().send(Distributor.DISTRIBUTE_EVENT, new JsonObject()
                                    .putString("path", publish.getTopic())
                                    .putValue("payload", optional));
                            ;
                        }

                        switch (publish.getQosLevel()) {
                            case AtMostOnce: {
                                break;
                            }
                            case AtLeastOnce: {
                                // This strategy basically requires persistence of the messages => How do we want to enable the persistence
                                // TODO Store message
                                // TODO Publish message to subscribers
                                // TODO Delete message
                                // TODO Send PUBACK
                                break;
                            }
                            case ExactlyOnce: {
                                // This strategy basically requires persistence of the messages => How do we want to enable the persistence
                            /* TODO Variant 1:
                             * Store message
                             * Send PUBREC
                             * After receive PUBREL
                             * Publish message to subscribers
                             * Delete message
                             * Send PUBCOMP
                             */

                            /* TODO Variant 2:
                             * Store messageId
                             * Publish message to subscribers
                             * Send PUBREC
                             * After receive PUBREL
                             * Delete message ID
                             * Send PUBCOMP
                             */
                                break;
                            }
                            case R3: {
                                throw new NotImplementedException();//"QoS Level R3 not implemented yet");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractAuthentication(Connect connect, Handler<Optional<JsonObject>> handler) {
        String clientId = connect.getClientId();
        Optional<String> username = connect.getUsername();
        Optional<String> password = connect.getPassword();
        if (username.isPresent() && password.isPresent()) {
            vertx.eventBus().send(Persistor.GET, Persistor.get(Path.of("/users")), new Handler<Message<JsonObject>>() {
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
                                if (username.get().equals(localUsername) && password.get().equals(localPassword)) {
                                    handler.handle(Optional.of(node));
                                }
                            }
                        }
                    }
                }
            });
        } else if (clientId != null) {
            vertx.eventBus().send(Persistor.GET, Persistor.get(Path.of("/users/" + clientId)), new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    JsonObject user = event.body();
                    handler.handle(Optional.ofNullable(user));
                }
            });
        }
    }
}
