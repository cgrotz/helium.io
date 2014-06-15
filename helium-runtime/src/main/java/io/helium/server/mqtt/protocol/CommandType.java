package io.helium.server.mqtt.protocol;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public enum CommandType {
    /**
     * Reserved
     */
    R0(0),

    /**
     * Client request to connect to Server
     */
    CONNECT(1),

    /**
     * Connect Acknowledgment
     */
    CONNACK(2),

    /**
     * Publish message
     */
    PUBLISH(3),

    /**
     * Publish Acknowledgment
     */
    PUBACK(4),

    /**
     * Publish Received (assured delivery part 1)
     */
    PUBREC(5),

    /**
     * Publish Release (assured delivery part 2)
     */
    PUBREL(6),

    /**
     * Publish Complete (assured delivery part 3)
     */
    PUBCOMP(7),

    /**
     * Client Subscribe request
     */
    SUBSCRIBE(8),

    /**
     * Subscribe Acknowledgment
     */
    SUBACK(9),

    /**
     * Client Unsubscribe request
     */
    UNSUBSCRIBE(10),

    /**
     * Unsubscribe Acknowledgment
     */
    UNSUBACK(11),

    /**
     * PING Request
     */
    PINGREQ(12),

    /**
     * PING Response
     */
    PINGRESP(13),

    /**
     * Client is Disconnecting
     */
    DISCONNECT(14),

    /**
     * Reserved
     */
    R15(15);


    private int value;

    private CommandType(int value) {
        this.value = value;
    }

    public static CommandType forValue(int value) {
        if (value == 0) {
            return R0;
        } else if (value == 1) {
            return CONNECT;
        } else if (value == 2) {
            return CONNACK;
        } else if (value == 3) {
            return PUBLISH;
        } else if (value == 4) {
            return PUBACK;
        } else if (value == 5) {
            return PUBREC;
        } else if (value == 6) {
            return PUBREL;
        } else if (value == 7) {
            return PUBCOMP;
        } else if (value == 8) {
            return SUBSCRIBE;
        } else if (value == 9) {
            return SUBACK;
        } else if (value == 10) {
            return UNSUBSCRIBE;
        } else if (value == 11) {
            return UNSUBACK;
        } else if (value == 12) {
            return PINGREQ;
        } else if (value == 13) {
            return PINGRESP;
        } else if (value == 14) {
            return DISCONNECT;
        } else if (value == 15) {
            return R15;
        }
        throw new IllegalArgumentException("Unknown CommandType: " + value);
    }
}
