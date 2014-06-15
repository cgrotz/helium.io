package io.helium.server.mqtt.decoder;

import io.helium.server.mqtt.protocol.Command;
import io.helium.server.mqtt.protocol.CommandType;
import io.helium.server.mqtt.protocol.QosLevel;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class DefaultDecoder implements Decoder {
    @Override
    public Optional<Command> decode(int startPos, int messageType, boolean dupFlag, int qosLevel, boolean retainFlag, int length, ByteBuf stream) {
        return Optional.of(new Command(CommandType.forValue(messageType), dupFlag, QosLevel.forValue(qosLevel), retainFlag, length));
    }
}
