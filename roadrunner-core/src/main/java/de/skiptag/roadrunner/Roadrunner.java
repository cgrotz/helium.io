package de.skiptag.roadrunner;

import java.io.File;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.RoadrunnerDisruptor;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.messaging.RoadrunnerSender;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

public class Roadrunner implements RoadrunnerSender {
    private InMemoryPersistence persistence;

    private RoadrunnerEventHandler roadrunnerEventHandler;

    private String path;

    private RoadrunnerDisruptor disruptor;

    private Set<RoadrunnerSender> senders = Sets.newHashSet();

    private RuleBasedAuthorization authorization;

    public Roadrunner(String journalDirectory, JSONObject rule) {
	try {
	    this.roadrunnerEventHandler = new RoadrunnerEventHandler(this);
	    this.authorization = new RuleBasedAuthorization(rule);
	    this.persistence = new InMemoryPersistence();

	    Optional<File> snapshotDirectory = Optional.absent();
	    this.disruptor = new RoadrunnerDisruptor(
		    new File(journalDirectory), snapshotDirectory, persistence,
		    authorization, roadrunnerEventHandler, true);
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    public Roadrunner(String journalDirectory) throws JSONException {
	this(
		journalDirectory,
		new JSONObject(
			"{\".write\": \"true\",\".read\": \"true\",\".remove\":\"true\"}"));
    }

    public void addSender(RoadrunnerSender webSocketServerHandler) {
	this.senders.add(webSocketServerHandler);
    }

    @Override
    public void send(String string) {
	for (RoadrunnerSender sender : senders) {
	    sender.send(string);
	}
    }

    public void handle(String msg) {
	try {
	    RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(msg, path);
	    Preconditions.checkArgument(roadrunnerEvent.has("type"), "No type defined in Event");
	    Preconditions.checkArgument(roadrunnerEvent.has("basePath"), "No basePath defined in Event");
	    if (roadrunnerEvent.getType() == RoadrunnerEventType.ATTACHED_LISTENER) {
		roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath());
		persistence.sync(new Path(roadrunnerEvent.extractNodePath()), roadrunnerEventHandler);
	    } else if (roadrunnerEvent.getType() == RoadrunnerEventType.DETACHED_LISTENER) {
		roadrunnerEventHandler.removeListener(roadrunnerEvent.extractNodePath());
	    } else {
		disruptor.handleEvent(roadrunnerEvent);
	    }
	} catch (Exception e) {
	    throw new RuntimeException(msg.toString(), e);
	}
    }

    public void setBasePath(String webSocketLocation) {
	path = webSocketLocation;
    }

    public Persistence getPersistence() {
	return persistence;
    }

    public void handleEvent(RoadrunnerEventType type, String nodePath,
	    Object value) throws JSONException {
	JSONObject message = new JSONObject();
	message.put("type", type.toString());
	message.put("path", nodePath);
	message.put("payload", value);
	RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(
		message.toString(), path);
	Preconditions.checkArgument(roadrunnerEvent.has("type"), "No type defined in Event");
	Preconditions.checkArgument(roadrunnerEvent.has("basePath"), "No basePath defined in Event");
	disruptor.handleEvent(roadrunnerEvent);
    }

    public void removeSender(RoadrunnerSender roadrunnerSender) {
	this.senders.remove(roadrunnerSender);
    }

    public Authorization getAuthorization() {
	return authorization;
    }
}
