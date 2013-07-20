package de.skiptag.roadrunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
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
    public static final JSONObject ALL_ACCESS_RULE = new JSONObject(
	    "{\".write\": \"true\",\".read\": \"true\",\".remove\":\"true\"}");

    private InMemoryPersistence persistence;

    private RoadrunnerEventHandler roadrunnerEventHandler;

    private String path;

    private RoadrunnerDisruptor disruptor;

    private Set<RoadrunnerSender> senders = Sets.newHashSet();

    private RuleBasedAuthorization authorization;

    public Roadrunner(String journalDirectory, JSONObject rule)
	    throws IOException {
	this.roadrunnerEventHandler = new RoadrunnerEventHandler(this);
	this.authorization = new RuleBasedAuthorization(rule);
	this.persistence = new InMemoryPersistence();

	Optional<File> snapshotDirectory = Optional.absent();
	this.disruptor = new RoadrunnerDisruptor(new File(journalDirectory),
		snapshotDirectory, persistence, authorization,
		roadrunnerEventHandler);
    }

    public Roadrunner(String journalDirectory) throws IOException {
	this(journalDirectory, ALL_ACCESS_RULE);
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
	RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(msg, path);
	if (roadrunnerEvent.getType() == RoadrunnerEventType.ATTACHED_LISTENER) {
	    roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath());
	    persistence.syncPath(new Path(roadrunnerEvent.extractNodePath()), roadrunnerEventHandler);
	} else if (roadrunnerEvent.getType() == RoadrunnerEventType.DETACHED_LISTENER) {
	    roadrunnerEventHandler.removeListener(roadrunnerEvent.extractNodePath());
	} else {
	    roadrunnerEvent.setFromHistory(false);
	    disruptor.handleEvent(roadrunnerEvent);
	}
    }

    public void setBasePath(String webSocketLocation) {
	path = webSocketLocation;
    }

    public Persistence getPersistence() {
	return persistence;
    }

    public void handleEvent(RoadrunnerEventType type, String nodePath,
	    Optional<?> value) {
	RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(type, nodePath,
		value, path);
	roadrunnerEvent.setFromHistory(false);
	disruptor.handleEvent(roadrunnerEvent);
    }

    public void removeSender(RoadrunnerSender roadrunnerSender) {
	this.senders.remove(roadrunnerSender);
    }

    public Authorization getAuthorization() {
	return authorization;
    }

    public String loadJsFile() throws IOException {
	URL uuid = Thread.currentThread()
		.getContextClassLoader()
		.getResource("uuid.js");
	URL reconnectingwebsocket = Thread.currentThread()
		.getContextClassLoader()
		.getResource("reconnecting-websocket.min.js");
	URL roadrunner = Thread.currentThread()
		.getContextClassLoader()
		.getResource("roadrunner.js");
	String uuidContent = com.google.common.io.Resources.toString(uuid, Charsets.UTF_8);
	String reconnectingWebsocketContent = com.google.common.io.Resources.toString(reconnectingwebsocket, Charsets.UTF_8);
	String roadrunnerContent = com.google.common.io.Resources.toString(roadrunner, Charsets.UTF_8);
	return uuidContent + "\r\n" + reconnectingWebsocketContent + "\r\n"
		+ roadrunnerContent;
    }
}
