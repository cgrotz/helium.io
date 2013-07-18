package de.skiptag.roadrunner.disruptor;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import journal.io.api.Journal;
import journal.io.api.Journal.ReadType;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventTranslator;
import de.skiptag.roadrunner.disruptor.processor.authorization.AuthorizationProcessor;
import de.skiptag.roadrunner.disruptor.processor.distribution.DistributionProcessor;
import de.skiptag.roadrunner.disruptor.processor.eventsourcing.EventSourceProcessor;
import de.skiptag.roadrunner.disruptor.processor.persistence.PersistenceProcessor;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.persistence.Persistence;

public class RoadrunnerDisruptor {
    private static final Logger logger = LoggerFactory.getLogger(RoadrunnerDisruptor.class);
    private static final int RING_SIZE = 256;
    private EventSourceProcessor eventSourceProcessor;
    private PersistenceProcessor persistenceProcessor;
    private DistributionProcessor distributionProcessor;
    private Disruptor<RoadrunnerEvent> disruptor;
    private AuthorizationProcessor authorizationProcessor;
    private Persistence persistence;

    private Optional<Journal> snapshotJournal = Optional.absent();

    @SuppressWarnings("unchecked")
    public RoadrunnerDisruptor(File journalDirectory,
	    Optional<File> snapshotDirectory, Persistence persistence,
	    Authorization authorization,
	    RoadrunnerEventHandler roadrunnerEventHandler,
	    boolean withDistribution) throws IOException {

	ExecutorService executor = Executors.newCachedThreadPool();
	disruptor = new Disruptor<RoadrunnerEvent>(
		RoadrunnerEvent.EVENT_FACTORY, RING_SIZE, executor);

	authorizationProcessor = new AuthorizationProcessor(authorization,
		persistence);
	eventSourceProcessor = new EventSourceProcessor(journalDirectory, this);
	persistenceProcessor = new PersistenceProcessor(persistence);
	distributionProcessor = new DistributionProcessor(persistence,
		authorization, roadrunnerEventHandler);

	EventHandlerGroup<RoadrunnerEvent> ehg = disruptor.handleEventsWith(authorizationProcessor)
		.then(eventSourceProcessor)
		.then(persistenceProcessor);
	if (withDistribution) {
	    ehg.then(distributionProcessor);
	}
	disruptor.start();

	if (snapshotDirectory.isPresent()) {
	    Journal journal = new Journal();
	    journal.setDirectory(snapshotDirectory.get());
	    snapshotJournal = Optional.fromNullable(journal);
	    journal.open();
	    Iterator<Location> iterator = journal.undo().iterator();
	    if (iterator.hasNext()) {
		Location lastEntryLocation = iterator.next();
		byte[] lastEntry = journal.read(lastEntryLocation, ReadType.SYNC);
		JSONObject snapshot = new JSONObject(new String(lastEntry));
		int pointer = snapshot.getInt("currentEventLogPointer");
		int dataFileId = snapshot.getInt("currentEventLogDataFileId");
		JSONObject payload = snapshot.getJSONObject("payload");
		persistence.restoreSnapshot(payload);
		eventSourceProcessor.setCurrentLocation(new Location(
			dataFileId, pointer));
	    }
	}

	eventSourceProcessor.restore();

	this.persistence = persistence;

    }

    public void handleEvent(final RoadrunnerEvent roadrunnerEvent) {

	logger.trace("handling event: " + roadrunnerEvent + "("
		+ roadrunnerEvent.length() + ")");

	Preconditions.checkArgument(roadrunnerEvent.has("type"), "No type defined in Event");
	Preconditions.checkArgument(roadrunnerEvent.has("basePath"), "No basePath defined in Event");
	RoadrunnerEventTranslator eventTranslator = new RoadrunnerEventTranslator(
		roadrunnerEvent);
	disruptor.publishEvent(eventTranslator);

    }

    public void snapshot() throws IOException, RuntimeException {
	Optional<Location> currentLocation = eventSourceProcessor.getCurrentLocation();
	if (snapshotJournal.isPresent() || currentLocation.isPresent()) {
	    Location location = currentLocation.get();
	    JSONObject payload = persistence.dumpSnapshot();
	    JSONObject snapshot = new JSONObject();
	    snapshot.put("currentEventLogPointer", location.getPointer());
	    snapshot.put("currentEventLogDataFileId", location.getDataFileId());
	    snapshot.put("payload", payload);
	    snapshotJournal.get().open();
	    snapshotJournal.get()
		    .write(snapshot.toString().getBytes(), WriteType.SYNC);
	    snapshotJournal.get().close();
	}
    }

    public void shutdown() {
	disruptor.shutdown();
    }
}
