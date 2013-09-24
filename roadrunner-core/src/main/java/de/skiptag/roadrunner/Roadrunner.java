package de.skiptag.roadrunner;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.RoadrunnerDisruptor;
import de.skiptag.roadrunner.disruptor.processor.distribution.Distributor;
import de.skiptag.roadrunner.event.RoadrunnerEvent;
import de.skiptag.roadrunner.event.RoadrunnerEventType;
import de.skiptag.roadrunner.event.changelog.ChangeLog;
import de.skiptag.roadrunner.json.Node;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

/**
 * 
 * Main entry point for Roadrunner
 * 
 * @author Christoph Grotz
 * 
 */
public class Roadrunner {

	private InMemoryPersistence			persistence;

	private RoadrunnerDisruptor			disruptor;

	private RuleBasedAuthorization	authorization;

	private Set<RoadrunnerEndpoint>	endpoints	= Sets.newHashSet();

	public Roadrunner(String basePath, Node rule, File journalDirectory,
			Optional<File> snapshotDirectory) throws IOException {
		checkNotNull(basePath);
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

	public void handle(RoadrunnerEvent roadrunnerEvent) {
		roadrunnerEvent.setFromHistory(false);
		this.disruptor.handleEvent(roadrunnerEvent);
	}

	public void handleEvent(RoadrunnerEventType type, String nodePath, Optional<?> value) {
		RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(type, nodePath, value);
		handle(roadrunnerEvent);
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
		URL rpc = Thread.currentThread().getContextClassLoader().getResource("rpc.js");
		URL reconnectingwebsocket = Thread.currentThread().getContextClassLoader()
				.getResource("reconnecting-websocket.min.js");
		URL roadrunner = Thread.currentThread().getContextClassLoader().getResource("roadrunner.js");

		String uuidContent = com.google.common.io.Resources.toString(uuid, Charsets.UTF_8);
		String reconnectingWebsocketContent = com.google.common.io.Resources.toString(
				reconnectingwebsocket, Charsets.UTF_8);
		String rpcContent = com.google.common.io.Resources.toString(rpc, Charsets.UTF_8);
		String roadrunnerContent = com.google.common.io.Resources.toString(roadrunner, Charsets.UTF_8);

		return uuidContent + "\r\n" + reconnectingWebsocketContent + "\r\n" + rpcContent + "\r\n"
				+ roadrunnerContent;
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

	public Distributor getDistributor() {
		return this.disruptor.getDistributor();
	}

	public Persistence getPersistence() {
		return this.persistence;
	}

	public Authorization getAuthorization() {
		return this.authorization;
	}

}
