package de.skiptag.roadrunner;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.RoadrunnerDisruptor;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.disruptor.event.changelog.ChangeLog;
import de.skiptag.roadrunner.json.Node;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;
import de.skiptag.roadrunner.queries.QueryEvaluator;

/**
 * 
 * Main entry point for Roadrunner
 * 
 * @author Christoph Grotz
 * 
 */
public class Roadrunner {

	private static final Logger			LOGGER		= LoggerFactory.getLogger(Roadrunner.class);

	private InMemoryPersistence			persistence;

	private RoadrunnerDisruptor			disruptor;

	private RuleBasedAuthorization	authorization;

	private Set<RoadrunnerEndpoint>	endpoints	= Sets.newHashSet();

	public Roadrunner(String basePath, Node rule, File journalDirectory,
			Optional<File> snapshotDirectory) throws IOException {
		checkNotNull(basePath);
		checkNotNull(rule);
		checkNotNull(journalDirectory);
		this.authorization = new RuleBasedAuthorization(rule);
		this.persistence = new InMemoryPersistence(this.authorization, this);

		this.disruptor = new RoadrunnerDisruptor(journalDirectory, snapshotDirectory, this.persistence,
				this.authorization);

	}

	public Roadrunner(String basePath, File journalDirectory, Optional<File> snapshotDirectory)
			throws IOException {
		this(checkNotNull(basePath), Authorization.ALL_ACCESS_RULE, checkNotNull(journalDirectory),
				snapshotDirectory);
	}

	public Roadrunner(String basePath) throws IOException {
		this(checkNotNull(basePath), Authorization.ALL_ACCESS_RULE, createTempDirectory().get(),
				createTempDirectory());
	}

	public void handle(RoadrunnerEndpoint roadrunnerEventHandler, RoadrunnerEvent roadrunnerEvent) {
		switch (roadrunnerEvent.getType())
			{
			case ATTACH_QUERY:
				handleAttachQuery(roadrunnerEventHandler, roadrunnerEvent);
				break;
			case DETACH_QUERY:
				handleDetachQuery(roadrunnerEventHandler, roadrunnerEvent);
				break;
			case ATTACHED_LISTENER:
				handleAttachedListener(roadrunnerEventHandler, roadrunnerEvent);
				break;
			case DETACHED_LISTENER:
				handleDetachedListener(roadrunnerEventHandler, roadrunnerEvent);
				break;
			case EVENT:
				handleEvent(roadrunnerEvent);
				break;
			case ONDISCONNECT:
				handleOnDisconnect(roadrunnerEventHandler, roadrunnerEvent);
				break;
			default:
				handleDefault(roadrunnerEvent);
				break;
			}
	}

	private void handleDefault(RoadrunnerEvent roadrunnerEvent) {
		roadrunnerEvent.setFromHistory(false);
		this.disruptor.handleEvent(roadrunnerEvent);
	}

	private void handleOnDisconnect(RoadrunnerEndpoint roadrunnerEventHandler,
			RoadrunnerEvent roadrunnerEvent) {
		roadrunnerEventHandler.registerDisconnectEvent(roadrunnerEvent);
	}

	private void handleEvent(RoadrunnerEvent roadrunnerEvent) {
		LOGGER.trace("Recevived Message: " + roadrunnerEvent.toString());
		this.disruptor.getDistributor().distribute(roadrunnerEvent);
	}

	private void handleDetachedListener(RoadrunnerEndpoint roadrunnerEventHandler,
			RoadrunnerEvent roadrunnerEvent) {
		roadrunnerEventHandler.removeListener(roadrunnerEvent.extractNodePath(),
				((Node) roadrunnerEvent.getPayload()).getString("type"));
	}

	private void handleAttachedListener(RoadrunnerEndpoint roadrunnerEventHandler,
			RoadrunnerEvent roadrunnerEvent) {
		roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath(),
				((Node) roadrunnerEvent.getPayload()).getString("type"));
		if ("child_added".equals(((Node) roadrunnerEvent.getPayload()).get("type"))) {
			this.persistence.syncPath(roadrunnerEvent.extractNodePath(), roadrunnerEventHandler);
		} else if ("value".equals(((Node) roadrunnerEvent.getPayload()).get("type"))) {
			this.persistence.syncPropertyValue(roadrunnerEvent.extractNodePath(), roadrunnerEventHandler);
		}
	}

	private void handleDetachQuery(RoadrunnerEndpoint roadrunnerEventHandler,
			RoadrunnerEvent roadrunnerEvent) {
		roadrunnerEventHandler.removeQuery(roadrunnerEvent.extractNodePath(),
				((Node) roadrunnerEvent.getPayload()).getString("query"));
	}

	private void handleAttachQuery(RoadrunnerEndpoint roadrunnerEventHandler,
			RoadrunnerEvent roadrunnerEvent) {
		roadrunnerEventHandler.addQuery(roadrunnerEvent.extractNodePath(),
				((Node) roadrunnerEvent.getPayload()).getString("query"));
		this.persistence.syncPathWithQuery(roadrunnerEvent.extractNodePath(), roadrunnerEventHandler,
				new QueryEvaluator(), ((Node) roadrunnerEvent.getPayload()).getString("query"));
	}

	public void handleEvent(RoadrunnerEventType type, String nodePath, Optional<?> value) {
		RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(type, nodePath, value);
		handleDefault(roadrunnerEvent);
	}

	public Persistence getPersistence() {
		return this.persistence;
	}

	public Authorization getAuthorization() {
		return this.authorization;
	}

	public void distributeChangeLog(ChangeLog changeLog) {
		for (RoadrunnerEndpoint endpoint : this.endpoints) {
			endpoint.distributeChangeLog(changeLog);
		}
	}

	public void addEndpoint(RoadrunnerEndpoint endpoint) {
		this.disruptor.addEndpoint(endpoint);
		this.endpoints.add(endpoint);
	}

	public void removeEndpoint(RoadrunnerEndpoint endpoint) {
		this.disruptor.removeEndpoint(endpoint);
		this.endpoints.remove(endpoint);
	}

	public static String loadJsFile() throws IOException {
		URL uuid = Thread.currentThread().getContextClassLoader().getResource("uuid.js");
		URL reconnectingwebsocket = Thread.currentThread().getContextClassLoader()
				.getResource("reconnecting-websocket.min.js");
		URL roadrunner = Thread.currentThread().getContextClassLoader().getResource("roadrunner.js");

		String uuidContent = com.google.common.io.Resources.toString(uuid, Charsets.UTF_8);
		String reconnectingWebsocketContent = com.google.common.io.Resources.toString(
				reconnectingwebsocket, Charsets.UTF_8);
		String roadrunnerContent = com.google.common.io.Resources.toString(roadrunner, Charsets.UTF_8);

		return uuidContent + "\r\n" + reconnectingWebsocketContent + "\r\n" + roadrunnerContent;
	}

	private static Optional<File> createTempDirectory() throws IOException {
		final File temp;
		temp = File.createTempFile("Temp" + System.currentTimeMillis(), "");
		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return Optional.fromNullable(temp);
	}
}
