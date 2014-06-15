package io.helium.server.mqtt.protocol;


/**
 * MQTT V3.1 Protocol Specification - section 3.2
 */
public enum ConnackCode {
    /**
     * Connection Accepted
     */
    Accepted(0),

    /**
     * Connection Refused: unacceptable protocol version
     */
    UnacceptableProtocolVersion(1),

    /**
     * Connection Refused: identifier rejected
     */
    IdentifierRejected(2),

    /**
     * Connection Refused: server unavailable
     */
    ServerUnavailable(3),

    /**
     * Connection Refused: bad user name or password
     */
    BadUserOrPassword(4),

    /**
     * Connection Refused: not authorized
     */
    NotAuthorized(5);

    // 6 - 255
    // Reserved for future use

    public int value;

    private ConnackCode(int value) {
        this.value = value;
    }

}
