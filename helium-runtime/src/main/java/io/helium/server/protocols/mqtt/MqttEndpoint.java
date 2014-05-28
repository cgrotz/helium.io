package io.helium.server.protocols.mqtt;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChildAddedLogEvent;
import io.helium.event.changelog.ValueChangedLogEvent;
import io.helium.json.Node;
import io.helium.server.Endpoint;
import io.helium.server.protocols.mqtt.encoder.Encoder;
import io.helium.server.protocols.mqtt.protocol.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.mapdb.DB;

import java.util.List;
import java.util.Set;

/**
 * Created by balu on 26.05.14.
 */
public class MqttEndpoint implements Endpoint {
    private final String clientId;
    private final ChannelHandlerContext channel;
    private final DB db;

    private Set<Topic> topics;
    private final Encoder encoder = new Encoder();
    private final PathMatcher pathMatcher = new PathMatcher();
    private long sequence = 0L;

    public MqttEndpoint(String clientId, ChannelHandlerContext channel, DB db) {
        this.clientId = clientId;
        this.channel = channel;
        this.db = db;

        this.topics = this.db.getHashSet(clientId + "Topics");
    }

    @Override
    public void distribute(HeliumEvent event) {
        distributeChangeLog(event.getChangeLog());
    }

    @Override
    public void distributeChangeLog(ChangeLog changeLog) {
        changeLog.getLog().forEach( logE -> {
            if (logE instanceof ChildAddedLogEvent) {
                ChildAddedLogEvent logEvent = (ChildAddedLogEvent) logE;
                if (hasListener(logEvent.getPath().append(logEvent.getName()), CHILD_ADDED)) {
                    fireChildAdded(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren(),
                            logEvent.getPrevChildName(), logEvent.getPriority());
                }
            }
    /*        if (logE instanceof ChildChangedLogEvent) {
                ChildChangedLogEvent logEvent = (ChildChangedLogEvent) logE;
                if (hasListener(logEvent.getPath(), CHILD_CHANGED)) {
                    fireChildChanged(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getHasChildren(), logEvent.getNumChildren(),
                            logEvent.getPrevChildName(), logEvent.getPriority());
                }
            }
            if (logE instanceof ChildRemovedLogEvent) {
                ChildRemovedLogEvent logEvent = (ChildRemovedLogEvent) logE;
                if (hasListener(logEvent.getPath(), CHILD_REMOVED)) {
                    fireChildRemoved(logEvent.getPath(), logEvent.getName(), logEvent.getValue());
                }
            }*/
            if (logE instanceof ValueChangedLogEvent) {
                ValueChangedLogEvent logEvent = (ValueChangedLogEvent) logE;
                if (hasListener(logEvent.getPath(), VALUE)) {
                    fireValue(logEvent.getName(), logEvent.getPath(), logEvent.getParent(),
                            logEvent.getValue(), logEvent.getPrevChildName(), logEvent.getPriority());
                }
            }
        });
    }

    private void fireChildAdded(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren, String prevChildName, int priority) {
        try {
            if(channel.channel().isWritable()) {
                ByteBuf buffer = Unpooled.buffer(1);
                encoder.encodePublish(
                        channel,
                        buffer,
                        (int) System.currentTimeMillis(), path.append(name).toString(), value.toString());
                channel.writeAndFlush(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fireChildChanged(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren, String prevChildName, int priority) {
        try {
            if(channel.channel().isWritable()) {
                ByteBuf buffer = Unpooled.buffer(1);
                encoder.encodePublish(
                        channel,
                        buffer,
                        (int) System.currentTimeMillis(), path.append(name).toString(), value.toString());
                channel.writeAndFlush(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fireValue(String name, Path path, Path parent, Object value, String prevChildName, int priority) {
        try {
            if(channel.channel().isWritable()) {
                ByteBuf buffer = Unpooled.buffer(1);
                encoder.encodePublish(
                        channel,
                        buffer,
                        (int) System.currentTimeMillis(), path.append(name).toString(), value.toString());
                channel.writeAndFlush(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fireChildRemoved(Path path, String name, Object value) {
        try {
            if(channel.channel().isWritable()) {
                ByteBuf buffer = Unpooled.buffer(1);
                encoder.encodePublish(
                        channel,
                        buffer,
                        (int) System.currentTimeMillis(), path.append(name).toString(), value.toString());
                channel.writeAndFlush(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasListener(Path path, String type) {
        return matchesSubscribedTopics(path);
    }

    @Override
    public void distributeEvent(Path path, Node data) {
        if(matchesSubscribedTopics(path)) {
            try {
                if(channel.channel().isWritable()) {
                    ByteBuf buffer = Unpooled.buffer(1);
                    encoder.encodePublish(
                            channel,
                            buffer,
                            (int) System.currentTimeMillis(), path.toString(), data.toString());
                    channel.writeAndFlush(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void subscribeToTopics(List<Topic> topics) {
        this.topics.addAll(topics);
    }

    public void unsubscribeToTopics(List<Topic> topics) {
        this.topics.removeAll(topics);
    }

    public boolean matchesSubscribedTopics(Path path) {
        for(Topic topic : topics) {
            if(pathMatcher.matchPath(topic.getPattern(), path))
            {
               return true;
            }
        }
        return false;
    }
}
