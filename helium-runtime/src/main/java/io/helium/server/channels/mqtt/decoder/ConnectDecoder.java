package io.helium.server.channels.mqtt.decoder;

import io.helium.server.channels.mqtt.protocol.Command;
import io.helium.server.channels.mqtt.protocol.CommandType;
import io.helium.server.channels.mqtt.protocol.Connect;
import io.helium.server.channels.mqtt.protocol.QosLevel;
import io.netty.buffer.ByteBuf;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class ConnectDecoder implements Decoder {
    @Override
    public Optional<Command> decode(int startPos, int messageType, boolean dupFlag, int qosLevel, boolean retainFlag, int length, ByteBuf stream) throws IOException {
        String protocolName = DataInputStream.readUTF(new DataInputStream(new ByteArrayInputStream(stream.readBytes(8).array())));
        short protocolVersion = stream.readUnsignedByte();

        // Connect flags
        // ------+------+-------+-------+-------+-------+-------+-------+---------+
        // bit   |   7  |   6   |   5   |   4   |   3   |   2   |   1   |    O    |
        // ------+------+-------+-------+-------+-------+-------+-------+---------+
        //       | User | Pass- | Will  |     Will      | Will  | Clean | Reserved|
        //       | Name |  word | Retain|     QoS       | Flag  |Session|         |
        // ------+------+-------+-------+---------------+-------+-------+---------+
        // 0b0000_0010: 0x02
        // 0b0000_0100: 0x04
        // 0b0001_1000: 0x18
        // 0b0010_0000: 0x20
        // 0b0100_0000: 0x40
        // 0b1000_0000: 0x80
        byte b = stream.readByte();
        boolean hasUsername = (((b & 0x80) >> 7) == 1);
        boolean hasPassword = (((b & 0x40) >> 6) == 1);
        boolean willRetain = (((b & 0x20) >> 5) == 1);
        int willQoS = (((b & 0x18) >> 3));
        boolean willFlag = (((b & 0x04) >> 2) == 1);
        boolean cleanSession = (((b & 0x02) >> 1) == 1);

        // The Keep Alive timer is a 16-bit value that represents the number of seconds for the time period.
        int keepAlive = (stream.readByte() << 8) + stream.readByte();

        // --- PAYLOAD
        String clientId = DataInputStream.readUTF(new BufferAsDataInput(stream));

    /*val willMessage =
      if (willFlag) {
        val willTopic = DataInputStream.readUTF(new BufferAsDataInput(stream))
        val willMessageBody = DataInputStream.readUTF(new BufferAsDataInput(stream))
        Some(Message(willTopic, willMessageBody, QosLevel(willQoS), willRetain))
      }
      else
        None
    */

    /*
    Note that, for compatibility with the original MQTT V3 specification,
    the Remaining Length field from the fixed header takes precedence over
    the User Name flag. Server implementations must allow for the possibility
    that the User Name flag is set, but the User Name string is missing.
    This is valid, and connections should be allowed to continue.

    Same for password...

    (MQTT V3.1 Protocol Specification - section 3.1)

    => one need to check if there is enough bytes remaining to read them both
     */
        Optional<String> username = Optional.empty();
        if (hasUsername && MqttDecoder.canReadMore(startPos, length, stream))
            username = Optional.of(DataInputStream.readUTF(new BufferAsDataInput(stream)));

        Optional<String> password = Optional.empty();
        if (hasPassword && MqttDecoder.canReadMore(startPos, length, stream))
            password = Optional.of(DataInputStream.readUTF(new BufferAsDataInput(stream)));

        Connect connect = new Connect(CommandType.forValue(messageType),
                dupFlag,
                QosLevel.forValue(qosLevel),
                retainFlag,
                length,
                protocolName,
                protocolVersion,
                cleanSession,
                keepAlive,
                clientId, username, password);

        return Optional.of(connect);
    }
}
