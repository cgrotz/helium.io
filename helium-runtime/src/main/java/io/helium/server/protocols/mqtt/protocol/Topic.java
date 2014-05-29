package io.helium.server.protocols.mqtt.protocol;


import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Created by Christoph Grotz on 25.05.14.
 */
public class Topic implements Serializable {
    private static final long serialVersionUID = 1L;

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

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("pattern", pattern).add("qos", qosLevel).toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Topic other = (Topic) obj;

        return   com.google.common.base.Objects.equal(this.pattern, other.pattern)
                && com.google.common.base.Objects.equal(this.qosLevel, other.qosLevel);
    }

    @Override
    public int hashCode()
    {
        return com.google.common.base.Objects.hashCode(
                this.pattern, this.qosLevel);
    }
}
