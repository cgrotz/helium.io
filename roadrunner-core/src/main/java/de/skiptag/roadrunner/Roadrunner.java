package de.skiptag.roadrunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.RoadrunnerDisruptor;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

public class Roadrunner {

    public static final JSONObject ALL_ACCESS_RULE = new JSONObject(
	    "{\".write\": \"true\",\".read\": \"true\",\".remove\":\"true\"}");

    private InMemoryPersistence persistence;

    private RoadrunnerDisruptor disruptor;

    private RuleBasedAuthorization authorization;

    public Roadrunner(String basePath, String journalDirectory,
	    Optional<File> snapshotDirectory, JSONObject rule)
	    throws IOException {
	this.authorization = new RuleBasedAuthorization(rule);
	this.persistence = new InMemoryPersistence();

	this.disruptor = new RoadrunnerDisruptor(new File(journalDirectory),
		snapshotDirectory, persistence, authorization);
    }

    public Roadrunner(String basePath, String journalDirectory,
	    Optional<File> snapshotDirectory) throws IOException {
	this(basePath, journalDirectory, snapshotDirectory, ALL_ACCESS_RULE);
    }

    public void handle(RoadrunnerEndpoint roadrunnerEventHandler,
	    RoadrunnerEvent roadrunnerEvent) {

	if (roadrunnerEvent.getType() == RoadrunnerEventType.ATTACHED_LISTENER) {
	    roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath());
	    if ("child_added".equals(((JSONObject) roadrunnerEvent.getPayload()).get("type"))) {
		persistence.syncPath(new Path(roadrunnerEvent.extractNodePath()), roadrunnerEventHandler);
	    } else if ("value".equals(((JSONObject) roadrunnerEvent.getPayload()).get("type"))) {
		persistence.syncPropertyValue(new Path(
			roadrunnerEvent.extractNodePath()), roadrunnerEventHandler);
	    }
	} else if (roadrunnerEvent.getType() == RoadrunnerEventType.DETACHED_LISTENER) {
	    roadrunnerEventHandler.removeListener(roadrunnerEvent.extractNodePath());
	} else if (roadrunnerEvent.getType() == RoadrunnerEventType.EVENT) {
	    disruptor.getDistributor().distribute(roadrunnerEvent);
	} else {
	    roadrunnerEvent.setFromHistory(false);
	    disruptor.handleEvent(roadrunnerEvent);
	}
    }

    public Persistence getPersistence() {
	return persistence;
    }

    public void handleEvent(RoadrunnerEventType type, String nodePath,
	    Optional<?> value) {
	RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(type, nodePath,
		value);
	roadrunnerEvent.setFromHistory(false);
	disruptor.handleEvent(roadrunnerEvent);
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

    public void addEndpoint(RoadrunnerEndpoint endpoint) {
	disruptor.addEndpoint(endpoint);
    }

    public void removeEndpoint(RoadrunnerEndpoint endpoint) {
	disruptor.removeEndpoint(endpoint);
    }
}
