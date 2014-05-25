package io.helium.server.protocols.mqtt;

import com.google.common.collect.Lists;
import io.helium.core.Core;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.server.protocols.mqtt.decoder.MqttDecoder;
import io.helium.server.protocols.mqtt.encoder.Encoder;
import io.helium.server.protocols.mqtt.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by balu on 25.05.14.
 */
public class MqttServerHandler extends ChannelHandlerAdapter {
    private static final Logger LOGGER = Logger.getLogger(MqttServerHandler.class.getName());

    private final Core core;
    private final Persistence persistence;
    private final Authorization authorization;
    private final MqttDecoder decoder;
    private final Encoder encoder = new Encoder();

    public MqttServerHandler(Persistence persistence, Authorization authorization, Core core) {
        this.persistence = persistence;
        this.authorization = authorization;
        this.core = core;
        this.decoder = new MqttDecoder();
    }


    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception { // (2)
        Optional<Command> command = decoder.decode((ByteBuf) msg);
        System.out.println(command);
        // Discard the received data silently.
        ((ByteBuf) msg).release(); // (3)
        if (command.isPresent()) {
            switch (command.get().getCommandType()) {
                case CONNECT: {
                    Connect connect = (Connect) command.get();
                    String clientId = connect.getClientId();
                    ctx.writeAndFlush(encoder.encodeConnack(ConnackCode.Accepted));
                    break;
                }
                case SUBSCRIBE: {
                    Subscribe subscribe = (Subscribe) command.get();

                    List<QosLevel> grantedQos = Lists.transform(subscribe.getTopics(), topic -> topic.getQosLevel());

                    // TODO subscribe

                    ctx.writeAndFlush(encoder.encodeSuback(subscribe.getMessageId(), grantedQos));
                    break;
                }
                /*case CommandType.UNSUBSCRIBE => {
                    val unsubscribe = command.get.asInstanceOf[Unsubscribe]
                    unsubscribe.topics.foreach( topic => topics.remove(topic))
                    sender ! Write(ByteString.apply(encoder.encodeUnsuback(unsubscribe.messageId).getBytes))
                }
                case CommandType.PINGREQ => {
                    log.debug("Pinreq command received")
                    remote ! Write(ByteString.apply(encoder.encodePingresp().getBytes))
                }*/
            }
        }
/*

        case CommandType.PUBLISH => {
            log.debug("Publish command received")
            val publish = command.get.asInstanceOf[Publish]
            publish.QoS match {
                case QosLevel.AtMostOnce => {
                    context.system.actorSelection("/user/mqtt/*") ! PublishedMessage(publish.messageId, publish.topic, publish.payload)
                }
                case QosLevel.AtLeastOnce => {
                    // This strategy basically requires persistence of the messages => How do we want to enable the persistence
                    // TODO Store message
                    // TODO Publish message to subscribers
                    // TODO Delete message
                    // TODO Send PUBACK
                }
                case QosLevel.ExactlyOnce => {
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
 /*               }
                case QosLevel.R3 => {
                    throw new NotImplementedError("QoS Level R3 not implemented yet")
                }
            }
        }
        case CommandType.DISCONNECT => {
            log.debug("Disconnect command received")
        }
*/
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
