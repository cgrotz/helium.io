package io.helium.server.protocols.mqtt.protocol;

import java.util.List;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class Subscribe extends Command {
    private final List<Topic> topics;
    private final int messageId;

    public Subscribe(CommandType commandType, boolean dupFlag, QosLevel qosLevel, boolean retainFlag, int length, int messageId, List<Topic> topics) {
        super(commandType, dupFlag, qosLevel, retainFlag, length);
        this.messageId = messageId;
        this.topics = topics;
    }

    public int getMessageId() {
        return messageId;
    }

    public List<Topic> getTopics() {
        return topics;
    }
}
