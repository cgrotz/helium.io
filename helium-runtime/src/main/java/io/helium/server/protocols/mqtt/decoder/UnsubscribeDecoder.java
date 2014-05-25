package io.helium.server.protocols.mqtt.decoder;

import com.google.common.collect.Lists;
import io.helium.server.protocols.mqtt.protocol.*;
import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Created by balu on 25.05.14.
 */
public class UnsubscribeDecoder implements Decoder {
    @Override
    public Optional<Command> decode(int startPos, int messageType, boolean dupFlag, int qosLevel, boolean retainFlag, int length, ByteBuf stream) throws IOException {
        int start = stream.readerIndex();
        // --- VARIABLE HEADER

        //  Message ID
        //  The variable header contains a Message ID because a
        //  SUBSCRIBE message has a QoS level of 1.
        // (MQTT V3.1 Protocol Specification - section 3.8)
        int messageId = stream.readUnsignedShort(); // 16-bit unsigned integer

        // --- PAYLOAD
        // The payload of a UNSUBSCRIBE message contains a list of
        // topic names to which the client wants to subscribe. The topic
        // strings may contain special Topic wildcard characters to represent a set
        // of topics. These topic/QoS pairs are packed contiguously (...)
        // (MQTT V3.1 Protocol Specification - section 3.8)
        List<Topic> topics = readTopicsUnsubscribe(startPos, length, stream);

        Unsubscribe unsubscribe = new Unsubscribe(CommandType.forValue(messageType),
                dupFlag,
                QosLevel.forValue(qosLevel),
                retainFlag,
                length,
                messageId,
                topics);

        return Optional.of(unsubscribe);
    }

    private List<Topic> readTopicsUnsubscribe(int startPos, int remainingLength, ByteBuf stream) throws IOException {
        return readTopicsUnsubscribe(Lists.newArrayList(), startPos, remainingLength, stream);
    }

    private List<Topic> readTopicsUnsubscribe(List<Topic> topics, int startPos, int remainingLength, ByteBuf stream) throws IOException {
        if (MqttDecoder.canReadMore(startPos, remainingLength, stream)) {
            String pattern = DataInputStream.readUTF(new BufferAsDataInput(stream));
            topics.add(new Topic(pattern, null));
            return topics;
        } else
            return topics;
    }
}
