package io.helium.server.protocols.mqtt.decoder;

import io.helium.server.protocols.mqtt.protocol.Command;
import io.helium.server.protocols.mqtt.protocol.CommandType;
import io.helium.server.protocols.mqtt.protocol.QosLevel;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

/**
 * Created by balu on 25.05.14.
 */
public class DefaultDecoder implements Decoder {
    @Override
    public Optional<Command> decode(int startPos, int messageType, boolean dupFlag, int qosLevel, boolean retainFlag, int length, ByteBuf stream) {
        return Optional.of(new Command(CommandType.forValue(messageType), dupFlag, QosLevel.forValue(qosLevel), retainFlag, length));
    }
}
