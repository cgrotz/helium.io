package de.skiptag.roadrunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.json.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.RoadrunnerDisruptor;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;
import de.skiptag.roadrunner.queries.QueryEvaluator;

public class Roadrunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(Roadrunner.class);

	private InMemoryPersistence persistence;

	private RoadrunnerDisruptor disruptor;

	private RuleBasedAuthorization authorization;

	public Roadrunner(String basePath, Node rule, File journalDirectory,
			Optional<File> snapshotDirectory) throws IOException {
		this.authorization = new RuleBasedAuthorization(rule);
		this.persistence = new InMemoryPersistence();

		this.disruptor = new RoadrunnerDisruptor(journalDirectory,
				snapshotDirectory, persistence, authorization);

	}

	public Roadrunner(String basePath, File journalDirectory,
			Optional<File> snapshotDirectory) throws IOException {
		this(basePath, Authorization.ALL_ACCESS_RULE, journalDirectory,
				snapshotDirectory);
	}

	public Roadrunner(String basePath) throws IOException {
		this(basePath, Authorization.ALL_ACCESS_RULE, createTempDirectory().get(),
				createTempDirectory());
	}

	public void handle(RoadrunnerEndpoint roadrunnerEventHandler,
			RoadrunnerEvent roadrunnerEvent) {

		if (roadrunnerEvent.getType() == RoadrunnerEventType.ATTACH_QUERY) {
			roadrunnerEventHandler.addQuery(roadrunnerEvent.extractNodePath(), ((Node) roadrunnerEvent.getPayload()).getString("query"));
			persistence.syncPathWithQuery(roadrunnerEvent.extractNodePath(), roadrunnerEventHandler, new QueryEvaluator(), ((Node) roadrunnerEvent.getPayload()).getString("query"));
		} else if (roadrunnerEvent.getType() == RoadrunnerEventType.DETACH_QUERY) {
			roadrunnerEventHandler.removeQuery(roadrunnerEvent.extractNodePath(), ((Node) roadrunnerEvent.getPayload()).getString("query"));

		} else if (roadrunnerEvent.getType() == RoadrunnerEventType.ATTACHED_LISTENER) {
			roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath(), ((Node) roadrunnerEvent.getPayload()).getString("type"));
			if ("child_added".equals(((Node) roadrunnerEvent.getPayload()).get("type"))) {
				persistence.syncPath(roadrunnerEvent.extractNodePath(), roadrunnerEventHandler);
			} else if ("value".equals(((Node) roadrunnerEvent.getPayload()).get("type"))) {
				persistence.syncPropertyValue(roadrunnerEvent.extractNodePath(), roadrunnerEventHandler);
			}
		} else if (roadrunnerEvent.getType() == RoadrunnerEventType.DETACHED_LISTENER) {
			roadrunnerEventHandler.removeListener(roadrunnerEvent.extractNodePath(), ((Node) roadrunnerEvent.getPayload()).getString("type"));
		} else if (roadrunnerEvent.getType() == RoadrunnerEventType.EVENT) {
			LOGGER.trace("Recevived Message: " + roadrunnerEvent.toString());
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
		RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(type, nodePath, value);
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

	private static Optional<File> createTempDirectory() throws IOException {
		final File temp;
		temp = File.createTempFile("Temp" + System.currentTimeMillis(), "");
		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: "
					+ temp.getAbsolutePath());
		}

		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: "
					+ temp.getAbsolutePath());
		}
		return Optional.fromNullable(temp);
	}

	public boolean hasBacklog() {
		return disruptor.hasBacklog();
	}
}
