package de.roadrunner.netty;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.Roadrunner;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.messaging.RoadrunnerSender;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

public class RoadrunnerStandalone implements RoadrunnerSender {
    private static Map<String, RoadrunnerStandalone> instances = Maps.newHashMap();
    private RoadrunnerEventHandler roadrunnerEventHandler;
    private InMemoryPersistence persistence;
    private Roadrunner disruptor;
    private Set<Channel> channels = Sets.newHashSet();
    private String path;

    private RoadrunnerStandalone(String path) {
	try {
	    this.path = path;
	    roadrunnerEventHandler = new RoadrunnerEventHandler(this, "");
	    Authorization authorization = new RuleBasedAuthorization(
		    new JSONObject());
	    this.persistence = new InMemoryPersistence(authorization);

	    Optional<File> snapshotDirectory = Optional.absent();
	    disruptor = new Roadrunner(new File(""), snapshotDirectory,
		    persistence, authorization, roadrunnerEventHandler, true);
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override
    public void send(String msg) {
	for (Channel channel : channels) {
	    channel.write(new TextWebSocketFrame(msg));
	}
    }

    public void addChannel(Channel channel) {
	this.channels.add(channel);
    }

    public void removeChannel(Channel channel) {
	this.channels.remove(channel);
    }

    public static RoadrunnerStandalone getInstance(String repositoryName) {
	RoadrunnerStandalone instance = instances.get(repositoryName);
	if (instance == null) {
	    instance = new RoadrunnerStandalone("http://localhost:8080/");
	    instances.put(repositoryName, instance);
	}
	return instance;
    }

    public void process(String msg) {
	try {
	    RoadrunnerEvent roadrunnerEvent;

	    try {
		roadrunnerEvent = new RoadrunnerEvent(msg, path, "");
		Preconditions.checkArgument(roadrunnerEvent.has("type"), "No type defined in Event");
		Preconditions.checkArgument(roadrunnerEvent.has("basePath"), "No basePath defined in Event");
		Preconditions.checkArgument(roadrunnerEvent.has("repositoryName"), "No repositoryName defined in Event");
	    } catch (Exception exp) {
		roadrunnerEvent = null;
	    }

	    if (roadrunnerEvent.has("type")) {
		if (roadrunnerEvent.getType() == RoadrunnerEventType.ATTACHED_LISTENER) {
		    roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath());
		    persistence.sync(new Path(roadrunnerEvent.extractNodePath()), roadrunnerEventHandler);
		} else if (roadrunnerEvent.getType() == RoadrunnerEventType.DETACHED_LISTENER) {
		    roadrunnerEventHandler.removeListener(roadrunnerEvent.extractNodePath());
		} else if (roadrunnerEvent.getType() == RoadrunnerEventType.QUERY) {
		    String query = roadrunnerEvent.getString("query");
		    // queryAction.handle(query);
		} else {
		    disruptor.handleEvent(roadrunnerEvent);
		}
	    }
	} catch (Exception e) {
	    throw new RuntimeException(msg.toString(), e);
	}
    }
}
