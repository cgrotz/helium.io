package io.helium.server.protocols.mqtt.protocol;

/**
 * MQTT V3.1 Protocol Specification - section 2.1
 * <p>
 * Created by balu on 25.05.14.
 */
public enum QosLevel {
    /**
     * At most once, Fire and Forget, <=1.
     * <p>
     * The message is delivered according to the best efforts of the underlying TCP/IP network.
     * A response is not expected and no retry semantics are defined in the protocol.
     * The message arrives at the server either once or not at all.
     * <p>
     * (MQTT V3.1 Protocol Specification - section 4.1)
     */
    AtMostOnce(0),

    /**
     * At least once, Acknowledged delivery, >=1.
     * <p>
     * The receipt of a message by the server is acknowledged by a PUBACK message.
     * If there is an identified failure of either the communications link or the
     * sending device, or the acknowledgement message is not received after a specified
     * period of time, the sender resends the message with the DUP bit set in the
     * message header. The message arrives at the server at least once.
     * Both SUBSCRIBE and UNSUBSCRIBE messages use QoS level 1.
     * <p>
     * If the client does not receive a PUBACK message (either within a time period
     * defined in the application, or if a failure is detected and the communications
     * session is restarted), the client may resend the PUBLISH message with the DUP
     * flag set.
     * When it receives a duplicate message from the client, the server republishes
     * the message to the subscribers, and sends another PUBACK message.
     * <p>
     * (MQTT V3.1 Protocol Specification - section 4.1)
     */
    AtLeastOnce(1),

    /**
     * Exactly once, Assured delivery, =1.
     * <p>
     * Additional protocol flows above QoS level 1 ensure that duplicate messages
     * are not delivered to the receiving application. This is the highest level
     * of delivery, for use when duplicate messages are not acceptable. There is
     * an increase in network traffic, but it is usually acceptable because of
     * the importance of the message content.
     * <p>
     * A message with QoS level 2 has a Message ID in the message header.
     * <p>
     * If a failure is detected, or after a defined time period, the protocol
     * flow is retried from the last unacknowledged protocol message; either
     * the PUBLISH or PUBREL.
     * <p>
     * (MQTT V3.1 Protocol Specification - section 4.1)
     */
    ExactlyOnce(2),

    /**
     * Reserved
     */
    R3(3);

    public int value;

    private QosLevel(int value) {
        this.value = value;
    }

    public static QosLevel forValue(int value) {
        if (value == 0) {
            return AtMostOnce;
        } else if (value == 1) {
            return AtLeastOnce;
        } else if (value == 2) {
            return ExactlyOnce;
        } else if (value == 3) {
            return R3;
        }
        throw new IllegalArgumentException("Unknown QoS Level: " + value);
    }
}
