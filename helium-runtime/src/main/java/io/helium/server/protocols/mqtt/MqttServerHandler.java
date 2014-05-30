package io.helium.server.protocols.mqtt;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.helium.common.Path;
import io.helium.core.Core;
import io.helium.event.HeliumEventType;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.server.DataTypeConverter;
import io.helium.server.protocols.mqtt.decoder.MqttDecoder;
import io.helium.server.protocols.mqtt.encoder.Encoder;
import io.helium.server.protocols.mqtt.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.mapdb.DB;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class MqttServerHandler extends ChannelHandlerAdapter {
    private static final Logger LOGGER = Logger.getLogger(MqttServerHandler.class.getName());

    private final Core core;
    private final Persistence persistence;
    private final Authorization authorization;
    private final MqttDecoder decoder;
    private final Encoder encoder = new Encoder();
    private final DB db;
    private Map<Channel, MqttEndpoint> endpoints  = Maps.newHashMap();

    public MqttServerHandler(Persistence persistence, Authorization authorization, Core core, DB db) {
        this.persistence = persistence;
        this.authorization = authorization;
        this.core = core;
        this.decoder = new MqttDecoder();
        this.db = db;
    }


    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception { // (2)
        Optional<Command> command = decoder.decode((ByteBuf) msg);
        // Discard the received data silently.
        ((ByteBuf) msg).release(); // (3)
        if (command.isPresent()) {
            switch (command.get().getCommandType()) {
                case CONNECT: {
                    Connect connect = (Connect) command.get();
                    String clientId = connect.getClientId();
                    Optional<Node> auth = extractAuthentication(connect);
                    ctx.writeAndFlush(encoder.encodeConnack(ConnackCode.Accepted));
                    MqttEndpoint endpoint = new MqttEndpoint(clientId, ctx, db, auth, persistence, authorization);
                    core.addEndpoint(endpoint);
                    endpoints.put(ctx.channel(), endpoint);
                    break;
                }
                case SUBSCRIBE: {
                    Subscribe subscribe = (Subscribe) command.get();

                    List<QosLevel> grantedQos = Lists.transform(subscribe.getTopics(), topic -> topic.getQosLevel());

                    endpoints.get(ctx.channel()).subscribeToTopics(subscribe.getTopics());
                    // TODO CG Subscriptions need to be made persistent
                    ctx.writeAndFlush(encoder.encodeSuback(subscribe.getMessageId(), grantedQos));
                    break;
                }
                case UNSUBSCRIBE: {
                    Unsubscribe unsubscribe = (Unsubscribe) command.get();
                    endpoints.get(ctx.channel()).unsubscribeToTopics(unsubscribe.getTopics());
                    ctx.writeAndFlush(encoder.encodeUnsuback(unsubscribe.getMessageId()));
                    break;
                }
                case PINGREQ: {
                    ctx.writeAndFlush(encoder.encodePingresp());
                    break;
                }
                case DISCONNECT: {
                    core.removeEndpoint(endpoints.remove(ctx.channel()));
                    break;
                }
                case PUBLISH: {
                    Publish publish = (Publish) command.get();
                    Optional<?> optional = Optional.ofNullable(DataTypeConverter.convert(publish.getArray()));
                    if(publish.isRetainFlag()) {
                        core.handleEvent(HeliumEventType.SET, endpoints.get(ctx.channel()).getAuth(), publish.getTopic(), optional);
                    }
                    else {
                        core.handleEvent(HeliumEventType.EVENT, endpoints.get(ctx.channel()).getAuth(), publish.getTopic(), optional);
                    }

                    switch(publish.getQosLevel()) {
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
                        case R3 : {
                            throw new NotImplementedException();//"QoS Level R3 not implemented yet");
                        }
                    }
                }
            }
        }
    }

    private Optional<Node> extractAuthentication(Connect connect) {
        String clientId = connect.getClientId();
        Optional<String> username = connect.getUsername();
        Optional<String> password = connect.getPassword();
        if(username.isPresent() && password.isPresent()) {
            Node users = persistence.getNode(new Path("/users"));
            for(Object value : users.values()) {
                if(value instanceof Node) {
                    Node node = (Node)value;
                    if(node.has("username") && node.has("password")) {
                        String localUsername = node.getString("username");
                        String localPassword = node.getString("password");
                        if(username.get().equals(localUsername) && password.get().equals(localPassword)) {
                            return Optional.of(node);
                        }
                    }
                }
            }
        }
        else if(clientId != null) {
            Node auth = persistence.getNode(Path.of("/users/"+clientId));
            if( auth != null){
                return Optional.of(auth);
            }
        }
        return Optional.empty();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        MqttEndpoint endpoint = endpoints.remove(ctx);
        core.removeEndpoint(endpoint);
        super.close(ctx, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
