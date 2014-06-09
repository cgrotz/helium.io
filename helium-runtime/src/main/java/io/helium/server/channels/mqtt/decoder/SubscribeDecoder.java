package io.helium.server.channels.mqtt.decoder;

import com.google.common.collect.Lists;
import io.helium.server.channels.mqtt.protocol.*;
import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class SubscribeDecoder implements Decoder {
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
        // The payload of a SUBSCRIBE message contains a list of
        // topic names to which the client wants to subscribe, and
        // the QoS level at which the client wants to receive the
        // messages. The strings are UTF-encoded, and the QoS level
        // occupies 2 bits of a single byte. The topic strings may
        // contain special Topic wildcard characters to represent a set
        // of topics. These topic/QoS pairs are packed contiguously (...)
        // (MQTT V3.1 Protocol Specification - section 3.8)
        List<Topic> topics = readTopics(start, length, stream);

        Subscribe subscribe = new Subscribe(CommandType.forValue(messageType),
                dupFlag,
                QosLevel.forValue(qosLevel),
                retainFlag,
                length,
                messageId,
                topics);

        return Optional.of(subscribe);
    }

    private List<Topic> readTopics(int startPos, int remainingLength, ByteBuf stream) throws IOException {
        return readTopics(Lists.newArrayList(), startPos, remainingLength, stream);
    }

    private List<Topic> readTopics(List<Topic> topics, int startPos, int remainingLength, ByteBuf stream) throws IOException {
        if (MqttDecoder.canReadMore(startPos, remainingLength, stream)) {
            String pattern = DataInputStream.readUTF(new BufferAsDataInput(stream));
            byte qos = stream.readByte();
            topics.add(new Topic(pattern, QosLevel.forValue(qos)));
            return readTopics(topics, startPos, remainingLength, stream);
        } else
            return topics;
    }
}
