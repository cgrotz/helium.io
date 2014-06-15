package io.helium.server.mqtt.decoder;

import io.helium.server.mqtt.protocol.Command;
import io.helium.server.mqtt.protocol.CommandType;
import io.helium.server.mqtt.protocol.Publish;
import io.helium.server.mqtt.protocol.QosLevel;
import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class PublishDecoder implements Decoder {
    @Override
    public Optional<Command> decode(int startPos, int messageType, boolean dupFlag, int qosLevel, boolean retainFlag, int length, ByteBuf stream) throws IOException {
        int start = stream.readerIndex();
        String topic = DataInputStream.readUTF(new BufferAsDataInput(stream));

        int messageId = 0;
        if (qosLevel == 1 || qosLevel == 2)
            messageId = stream.readUnsignedShort(); // 16-bit unsigned integer

        int payloadPos = stream.readerIndex();
        int payloadLen = length - (payloadPos - start);
        ByteBuf payload = stream.readBytes(payloadLen);

        Publish publish = new Publish(CommandType.forValue(messageType),
                dupFlag,
                QosLevel.forValue(qosLevel),
                retainFlag,
                length,
                messageId,
                topic,
                payload.array());

        return Optional.of(publish);
    }
}
