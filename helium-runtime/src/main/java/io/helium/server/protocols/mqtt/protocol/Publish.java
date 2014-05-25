package io.helium.server.protocols.mqtt.protocol;

/**
 * Created by balu on 25.05.14.
 */
public class Publish extends Command {
    private final byte[] array;
    private final String topic;
    private final int messageId;

    public Publish(CommandType commandType,
                   boolean dupFlag,
                   QosLevel qosLevel,
                   boolean retainFlag,
                   int length,
                   int messageId,
                   String topic,
                   byte[] array) {
        super(commandType, dupFlag, qosLevel, retainFlag, length);
        this.messageId = messageId;
        this.topic = topic;
        this.array = array;
    }

    public byte[] getArray() {
        return array;
    }

    public String getTopic() {
        return topic;
    }

    public int getMessageId() {
        return messageId;
    }
}
