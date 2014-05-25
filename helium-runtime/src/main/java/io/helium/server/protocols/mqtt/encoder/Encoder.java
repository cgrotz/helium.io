package io.helium.server.protocols.mqtt.encoder;

import com.google.common.base.Charsets;
import io.helium.server.protocols.mqtt.protocol.ConnackCode;
import io.helium.server.protocols.mqtt.protocol.QosLevel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

/**
 * Created by balu on 25.05.14.
 */
public class Encoder {
    public ByteBuf encodeConnack(ConnackCode connackCode) {
        //
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // bit     |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0   |
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // byte 1  |  0     0     1     0  |  x  |  x     x  |  x   |
        // --------+-----------------------+-----+-----------+------+
        // byte 2  |              Remaining Length                  |
        //---------+------------------------------------------------+
        // The DUP, QoS and RETAIN flags are not used in the CONNACK message.
        // MQTT V3.1 Protocol Specification - sections 3.2
        ByteBuf buffer = Unpooled.buffer(4);

        // byte 1: 0b_0010_0000 = 0x20
        buffer.writeByte(0x20);
        // byte 2: remaining length = 2 => 0x02
        buffer.writeByte(0x02);
        // 1st byte; unused => 0x00
        buffer.writeByte(0x00);
        // 2nd byte: connack return code
        buffer.writeByte(connackCode.value);

        return buffer;
    }

    public ByteBuf encodePuback(int messageId) {
        //
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // bit     |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0   |
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // byte 1  |  0     1     0     0  |  x  |  x     x  |  x   |
        // --------+-----------------------+-----+-----------+------+
        // byte 2  |              Remaining Length                  |
        //---------+------------------------------------------------+
        // The DUP, QoS and RETAIN flags are not used in the PUBACK message.
        // MQTT V3.1 Protocol Specification - sections 3.4
        ByteBuf buffer = Unpooled.buffer(4);

        // byte 1: 0b_0100_0000 = 0x40
        buffer.writeByte(0x40);
        // byte 2: remaining length = 2 => 0x02
        buffer.writeByte(0x02);

        buffer.writeShort((messageId & 0xFFFF));

        return buffer;
    }

    public ByteBuf encodePubrec(int messageId) {
        //
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // bit     |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0   |
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // byte 1  |  0     1     0     1  |  x  |  x     x  |  x   |
        // --------+-----------------------+-----+-----------+------+
        // byte 2  |              Remaining Length                  |
        //---------+------------------------------------------------+
        // The DUP, QoS and RETAIN flags are not used in the PUBREC message.
        // MQTT V3.1 Protocol Specification - sections 3.5

        ByteBuf buffer = Unpooled.buffer(4);

        // byte 1: 0b_0101_0000 = 0x50
        buffer.writeByte(0x50);
        // byte 2: remaining length = 2 => 0x02
        buffer.writeByte(0x02);

        buffer.writeShort((messageId & 0xFFFF));

        return buffer;
    }

    public ByteBuf encodeSuback(int messageId, List<QosLevel> grantedQos) {
        //
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // bit     |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0   |
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // byte 1  |  1     0     0     1  |  x  |  x     x  |  x   |
        // --------+-----------------------+-----+-----------+------+
        // byte 2  |              Remaining Length                  |
        //---------+------------------------------------------------+
        // The DUP, QoS and RETAIN flags are not used in the SUBACK message.
        // MQTT V3.1 Protocol Specification - sections 3.9

        // write payload first to calculate the 'remaining length'
        ByteBuf content = Unpooled.buffer(grantedQos.size() + 2);

        content.writeShort((messageId & 0xFFFF));

        grantedQos.forEach(
                qos -> content.writeByte(qos.value & 0xF)
        );

        long len = grantedQos.size() + 2;


        ByteBuf header = Unpooled.buffer(2);
        // byte 1: 0b_1001_0000 = 0x90
        header.writeByte(0x90);
        encodeRemainingLength(len, header);

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(header);
        buffer.writeBytes(content);
        return buffer;
    }

    public ByteBuf encodePingresp() {
        //
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // bit     |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0   |
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // byte 1  |  1     0     0     1  |  x  |  x     x  |  x   |
        // --------+-----------------------+-----+-----------+------+
        // byte 2  |              Remaining Length                  |
        //---------+------------------------------------------------+
        // The DUP, QoS and RETAIN flags are not used in the PINGRESP message.
        // MQTT V3.1 Protocol Specification - sections 3.9

        ByteBuf header = Unpooled.buffer(2);
        // byte 1: 0b_1101_0000 = 0x90
        header.writeByte(0xD0);
        // byte 2: remaining length = 2 => 0x02
        header.writeByte(0x00);

        return header;
    }

    public ByteBuf encodeUnsuback(int messageId) {
        //
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // bit     |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0   |
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // byte 1  |  0     1     0     1  |  x  |  x     x  |  x   |
        // --------+-----------------------+-----+-----------+------+
        // byte 2  |              Remaining Length                  |
        //---------+------------------------------------------------+
        // The DUP, QoS and RETAIN flags are not used in the UNSUBACK message.
        // MQTT V3.1 Protocol Specification - sections 3.5

        ByteBuf buffer = Unpooled.buffer(4);

        // byte 1: 0b_1011_0000 = 0x50
        buffer.writeByte(0xB0);
        // byte 2: remaining length = 2 => 0x02
        buffer.writeByte(0x02);

        // variable header:
        // Contains the Message Identifier (Message ID) for the PUBLISH
        // message that is being acknowledged.
        buffer.writeShort(messageId & 0xFFFF); // 16-bit unsigned integer

        return buffer;
    }

    public ByteBuf encodePublish(int messageId, String topic, byte[] payload) {
        //
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // bit     |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0   |
        // --------+-----+-----+-----+-----+-----+-----+-----+------+
        // byte 1  |  0     1     0     1  |  x  |  x     x  |  x   |
        // --------+-----------------------+-----+-----------+------+
        // byte 2  |              Remaining Length                  |
        //---------+------------------------------------------------+
        // The DUP, QoS and RETAIN flags are not used in the PUBLISH message.
        // MQTT V3.1 Protocol Specification - sections 3.5

        ByteBuf content = Unpooled.buffer();

        content.writeShort((messageId & 0xFFFF));
        content.writeBytes(topic.getBytes(Charsets.UTF_8));
        content.writeBytes(payload);

        long len = content.capacity();

        ByteBuf header = Unpooled.buffer(2);
        // byte 1: 0b_0011_0000 = 0x90
        header.writeByte(0x30);

        encodeRemainingLength(len, header);

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(header);
        buffer.writeBytes(content);
        return buffer;
    }

    public void encodeRemainingLength(long x, ByteBuf buffer) {
        int digit = (int) (x % 128);
        int newX = (int) (x / 128);
        if (newX > 0) {
            buffer.writeByte((digit | 0x80));
            encodeRemainingLength(newX, buffer);
        } else {
            buffer.writeByte(digit);
        }
    }
}
