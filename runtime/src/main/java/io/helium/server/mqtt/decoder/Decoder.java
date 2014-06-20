package io.helium.server.mqtt.decoder;

import io.helium.server.mqtt.protocol.Command;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public interface Decoder {
    Optional<Command> decode(int startPos, int messageType, boolean dupFlag, int qosLevel, boolean retainFlag, int length, ByteBuf stream) throws IOException;
}
