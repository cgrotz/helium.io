package io.helium.server.mqtt.decoder;

import io.helium.server.mqtt.protocol.Command;
import io.helium.server.mqtt.protocol.CommandType;
import io.netty.buffer.ByteBuf;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class MqttDecoder {
    ConnectDecoder connectDecoder = new ConnectDecoder();
    SubscribeDecoder subscribeDecoder = new SubscribeDecoder();
    UnsubscribeDecoder unsubscribeDecoder = new UnsubscribeDecoder();
    PublishDecoder publishDecoder = new PublishDecoder();
    DefaultDecoder defaultDecoder = new DefaultDecoder();

    public Optional<Command> decode(Buffer buffer) throws IOException {
        ByteBuf stream = buffer.getByteBuf();
        if (stream.readableBytes() < 2) {
            // TODO noComplete Header
        } else {
            int startPos = stream.readerIndex();
            byte b1 = stream.readByte();
            int messageType = ((b1 & 0x00F0) >> 4);
            boolean dupFlag = (((b1 & 0x0008) >> 3) == 1);
            int qosLevel = ((b1 & 0x0006) >> 1);
            boolean retainFlag = ((b1 & 0x0001) == 1);
            int length = extractLength(stream);

            while (stream.readableBytes() < length) {
                // Waiting on rest of package
            }

            CommandType commandType = CommandType.forValue(messageType);

            switch (commandType) {
                case CONNECT: {
                    return connectDecoder.decode(startPos, messageType, dupFlag, qosLevel, retainFlag, length, stream);
                }
                case PUBLISH: {
                    return publishDecoder.decode(startPos, messageType, dupFlag, qosLevel, retainFlag, length, stream);
                }
                case SUBSCRIBE: {
                    return subscribeDecoder.decode(startPos, messageType, dupFlag, qosLevel, retainFlag, length, stream);
                }
                case UNSUBSCRIBE: {
                    return unsubscribeDecoder.decode(startPos, messageType, dupFlag, qosLevel, retainFlag, length, stream);
                }
                default: {
                    return defaultDecoder.decode(startPos, messageType, dupFlag, qosLevel, retainFlag, length, stream);
                }
            }
        }
        return Optional.empty();
    }

    public static int extractLength(ByteBuf stream) {
        byte length1 = stream.readByte();
        if (length1 > 127) {
            byte length2 = stream.readByte();
            if (length2 > 127) {
                byte length3 = stream.readByte();
                if (length3 > 127) {
                    byte length4 = stream.readByte();
                    return (length1 << 32) + (length2 << 16) + (length3 << 8) + length4;
                } else {
                    return (length1 << 16) + (length2 << 8) + length3;
                }
            } else {
                return (length1 << 8) + length2;
            }
        } else {
            return length1;
        }
    }

    /**
     * Indicates whether or not the buffer contains enough bytes according to the
     * 'remaining length' defined in the header.
     */
    public static boolean canReadMore(int startPos, int remainingLength, ByteBuf stream) {
        int currentPos = stream.readerIndex();
        return ((currentPos - startPos) < remainingLength);
    }
}
