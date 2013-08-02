package de.skiptag.roadrunner.disruptor;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import journal.io.api.ClosedJournalException;
import journal.io.api.CompactedDataFileException;
import journal.io.api.Journal;
import journal.io.api.Journal.ReadType;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;

import org.json.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventTranslator;
import de.skiptag.roadrunner.disruptor.processor.authorization.AuthorizationProcessor;
import de.skiptag.roadrunner.disruptor.processor.distribution.DistributionProcessor;
import de.skiptag.roadrunner.disruptor.processor.eventsourcing.EventSourceProcessor;
import de.skiptag.roadrunner.disruptor.processor.persistence.PersistenceProcessor;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.persistence.Persistence;

public class RoadrunnerDisruptor implements ExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RoadrunnerDisruptor.class);
    private static final int RING_SIZE = 256;
    private EventSourceProcessor eventSourceProcessor;
    private PersistenceProcessor persistenceProcessor;
    private DistributionProcessor distributionProcessor;

    private Disruptor<RoadrunnerEvent> disruptor;

    public Disruptor<RoadrunnerEvent> getDisruptor() {
	return disruptor;
    }

    private AuthorizationProcessor authorizationProcessor;
    private Persistence persistence;

    private Optional<Journal> snapshotJournal = Optional.absent();
    private long currentSequence;

    public RoadrunnerDisruptor(File journalDirectory,
	    Optional<File> snapshotDirectory, Persistence persistence,
	    Authorization authorization) throws IOException {

	this.persistence = persistence;
	initDisruptor(journalDirectory, persistence, authorization);
	if (snapshotDirectory.isPresent()) {
	    restoreFromSnapshot(snapshotDirectory.get(), persistence);
	} else {
	    restoreFromJournal();
	}
    }

    private void restoreFromJournal() throws ClosedJournalException,
	    CompactedDataFileException, IOException {
	eventSourceProcessor.restore();
    }

    private void restoreFromSnapshot(File snapshotDirectory,
	    Persistence persistence) throws IOException,
	    ClosedJournalException, CompactedDataFileException {

	Journal journal = new Journal();
	journal.setDirectory(snapshotDirectory);
	snapshotJournal = Optional.fromNullable(journal);
	journal.open();
	Iterator<Location> iterator = journal.undo().iterator();
	if (iterator.hasNext()) {
	    Location lastEntryLocation = iterator.next();
	    byte[] lastEntry = journal.read(lastEntryLocation, ReadType.SYNC);
	    Node snapshot = new Node(new String(lastEntry));
	    int pointer = snapshot.getInt("currentEventLogPointer");
	    int dataFileId = snapshot.getInt("currentEventLogDataFileId");
	    Node payload = snapshot.getNode(RoadrunnerEvent.PAYLOAD);
	    Node node = new Node();
	    node.populate(null, node);
	    persistence.restoreSnapshot(node);
	    eventSourceProcessor.setCurrentLocation(new Location(dataFileId,
		    pointer));
	}
	restoreFromJournal();
    }

    @SuppressWarnings("unchecked")
    private void initDisruptor(File journalDirectory, Persistence persistence,
	    Authorization authorization) throws IOException {
	ExecutorService executor = Executors.newCachedThreadPool();
	disruptor = new Disruptor<RoadrunnerEvent>(
		RoadrunnerEvent.EVENT_FACTORY, RING_SIZE, executor);

	authorizationProcessor = new AuthorizationProcessor(authorization,
		persistence);
	eventSourceProcessor = new EventSourceProcessor(journalDirectory, this);
	persistenceProcessor = new PersistenceProcessor(persistence);
	distributionProcessor = new DistributionProcessor();

	disruptor.handleExceptionsWith(this);
	disruptor.handleEventsWith(authorizationProcessor)
		.then(eventSourceProcessor)
		.then(persistenceProcessor)
		.then(distributionProcessor);

	disruptor.start();
    }

    public void handleEvent(final RoadrunnerEvent roadrunnerEvent) {
	Preconditions.checkArgument(roadrunnerEvent.has(RoadrunnerEvent.TYPE), "No type defined in Event");
	RoadrunnerEventTranslator eventTranslator = new RoadrunnerEventTranslator(
		roadrunnerEvent);
	logger.trace("handling event: " + roadrunnerEvent + "("
		+ roadrunnerEvent.length() + ")");
	disruptor.publishEvent(eventTranslator);
	this.currentSequence = eventTranslator.getSequence();
    }

    public void snapshot() throws IOException, RuntimeException {
	Optional<Location> currentLocation = eventSourceProcessor.getCurrentLocation();
	if (snapshotJournal.isPresent() || currentLocation.isPresent()) {
	    Location location = currentLocation.get();
	    Node payload = persistence.dumpSnapshot();
	    Node snapshot = new Node();
	    snapshot.put("currentEventLogPointer", location.getPointer());
	    snapshot.put("currentEventLogDataFileId", location.getDataFileId());
	    snapshot.put(RoadrunnerEvent.PAYLOAD, payload);
	    snapshotJournal.get().open();
	    snapshotJournal.get()
		    .write(snapshot.toString().getBytes(), WriteType.SYNC);
	    snapshotJournal.get().close();
	}
    }

    public void shutdown() {
	disruptor.shutdown();
    }

    public void addEndpoint(RoadrunnerEndpoint handler) {
	distributionProcessor.addHandler(handler);
    }

    public void removeEndpoint(RoadrunnerEndpoint handler) {
	distributionProcessor.removeHandler(handler);
    }

    public DistributionProcessor getDistributor() {
	return distributionProcessor;
    }

    public boolean hasBacklog() {
	return currentSequence != distributionProcessor.getSequence();
    }

    @Override
    public void handleEventException(Throwable ex, long sequence, Object event) {
	logger.error("Event Exception (msg: " + ex.getMessage()
		+ ", sequence: +" + sequence + ", event: " + event + ")", ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
	logger.error("OnStart Exception (msg: " + ex.getMessage() + ")", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
	logger.error("OnShutdown Exception (msg: " + ex.getMessage() + ")", ex);
    }
}
