package io.helium.server.protocols.mqtt.protocol;

/**
 * Created by balu on 25.05.14.
 */
public class Topic {
    private final String pattern;
    private final QosLevel qosLevel;

    public Topic(String pattern, QosLevel qosLevel) {
        this.pattern = pattern;
        this.qosLevel = qosLevel;
    }

    public String getPattern() {
        return pattern;
    }

    public QosLevel getQosLevel() {
        return qosLevel;
    }
}
