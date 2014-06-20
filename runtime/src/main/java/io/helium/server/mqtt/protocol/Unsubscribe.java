package io.helium.server.mqtt.protocol;

import java.util.List;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class Unsubscribe extends Command {
    private final List<Topic> topics;
    private final int messageId;

    public Unsubscribe(CommandType commandType, boolean dupFlag, QosLevel qosLevel, boolean retainFlag, long remainingLength, int messageId, List<Topic> topics) {
        super(commandType, dupFlag, qosLevel, retainFlag, remainingLength);
        this.messageId = messageId;
        this.topics = topics;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public int getMessageId() {
        return messageId;
    }
}
