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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;

import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.processor.authorization.AuthorizationProcessor;
import de.skiptag.roadrunner.disruptor.processor.distribution.DistributionProcessor;
import de.skiptag.roadrunner.disruptor.processor.eventsourcing.EventSourceProcessor;
import de.skiptag.roadrunner.disruptor.processor.storage.StorageProcessor;

public class DisruptorRoadrunnerService {
    private static final Logger logger = LoggerFactory.getLogger(DisruptorRoadrunnerService.class);
    private static final int RING_SIZE = 256;
    private EventSourceProcessor eventSourceProcessor;
    private StorageProcessor storageProcessor;
    private DistributionProcessor distributionProcessor;
    private Disruptor<RoadrunnerEvent> disruptor;
    private AuthorizationProcessor authorizationProcessor;
    private DataService dataService;

    private Optional<Journal> snapshotJournal = Optional.absent();

    @SuppressWarnings("unchecked")
    public DisruptorRoadrunnerService(File journalDirectory,
	    Optional<File> snapshotDirectory, DataService dataService,
	    AuthorizationService authorizationService, boolean withDistribution)
	    throws IOException, JSONException {

	ExecutorService executor = Executors.newCachedThreadPool();
	disruptor = new Disruptor<RoadrunnerEvent>(
		RoadrunnerEvent.EVENT_FACTORY, RING_SIZE, executor);

	authorizationProcessor = new AuthorizationProcessor(
		authorizationService);
	eventSourceProcessor = new EventSourceProcessor(journalDirectory, this);
	storageProcessor = new StorageProcessor(dataService);
	distributionProcessor = new DistributionProcessor(dataService);

	EventHandlerGroup<RoadrunnerEvent> ehg = disruptor.handleEventsWith(authorizationProcessor)
		.then(eventSourceProcessor)
		.then(storageProcessor);
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
		dataService.restoreSnapshot(payload);
		eventSourceProcessor.setCurrentLocation(new Location(
			dataFileId, pointer));
	    }
	}

	eventSourceProcessor.restore();

	this.dataService = dataService;

    }

    public void handleEvent(final RoadrunnerEvent roadrunnerEvent) {

	logger.trace("handling event: " + roadrunnerEvent + "("
		+ roadrunnerEvent.length() + ")");
	try {
	    Preconditions.checkArgument(roadrunnerEvent.has("type"), "No type defined in Event");
	    Preconditions.checkArgument(roadrunnerEvent.has("basePath"), "No basePath defined in Event");
	    Preconditions.checkArgument(roadrunnerEvent.has("repositoryName"), "No repositoryName defined in Event");
	    RoadrunnerEventTranslator eventTranslator = new RoadrunnerEventTranslator(
		    roadrunnerEvent);
	    disruptor.publishEvent(eventTranslator);
	} catch (Exception exp) {
	    logger.warn("Error in message (" + exp.getMessage() + "): "
		    + roadrunnerEvent);
	}

    }

    public void snapshot() throws IOException, JSONException {
	Optional<Location> currentLocation = eventSourceProcessor.getCurrentLocation();
	if (snapshotJournal.isPresent() || currentLocation.isPresent()) {
	    Location location = currentLocation.get();
	    JSONObject payload = dataService.dumpSnapshot();
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
