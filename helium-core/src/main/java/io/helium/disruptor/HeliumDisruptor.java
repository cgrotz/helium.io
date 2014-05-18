package io.helium.disruptor;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;

import io.helium.authorization.Authorization;
import io.helium.disruptor.processor.authorization.AuthorizationProcessor;
import io.helium.disruptor.processor.distribution.Distributor;
import io.helium.disruptor.processor.eventsourcing.EventSourceProcessor;
import io.helium.disruptor.processor.persistence.PersistenceProcessor;
import io.helium.disruptor.translator.HeliumEventTranslator;
import io.helium.event.HeliumEvent;
import io.helium.json.Node;
import io.helium.messaging.HeliumEndpoint;
import io.helium.persistence.Persistence;

public class HeliumDisruptor implements ExceptionHandler {

	public static final EventFactory<HeliumEvent>	EVENT_FACTORY	= new EventFactory<HeliumEvent>() {

																																		@Override
																																		public HeliumEvent newInstance() {
																																			return new HeliumEvent();
																																		}
																																	};
	private static final Logger												logger				= LoggerFactory
																																			.getLogger(HeliumDisruptor.class);
	private static final int													RING_SIZE			= 256;
	private EventSourceProcessor											eventSourceProcessor;
	private PersistenceProcessor											persistenceProcessor;
	private Distributor																distributionProcessor;

	private Disruptor<HeliumEvent>								disruptor;

	public Disruptor<HeliumEvent> getDisruptor() {
		return disruptor;
	}

	private AuthorizationProcessor	authorizationProcessor;
	private Persistence							persistence;

	private Optional<Journal>				snapshotJournal	= Optional.absent();
	private long										currentSequence;

	public HeliumDisruptor(File journalDirectory, Optional<File> snapshotDirectory,
			Persistence persistence, Authorization authorization) throws IOException {

		this.persistence = persistence;
		initDisruptor(journalDirectory, persistence, authorization);
		if (snapshotDirectory.isPresent()) {
			restoreFromSnapshot(snapshotDirectory.get(), persistence);
		} else {
			restoreFromJournal();
		}
	}

	private void restoreFromJournal() throws ClosedJournalException, CompactedDataFileException,
			IOException {
		eventSourceProcessor.restore();
	}

	private void restoreFromSnapshot(File snapshotDirectory, Persistence persistence)
			throws IOException, ClosedJournalException, CompactedDataFileException {

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
			Node node = new Node();
			node.populate(null, node);
			persistence.restoreSnapshot(node);
			eventSourceProcessor.setCurrentLocation(new Location(dataFileId, pointer));
		}
		restoreFromJournal();
	}

	@SuppressWarnings("unchecked")
	private void initDisruptor(File journalDirectory, Persistence persistence,
			Authorization authorization) throws IOException {
		ExecutorService executor = Executors.newCachedThreadPool();
		disruptor = new Disruptor<HeliumEvent>(HeliumDisruptor.EVENT_FACTORY, RING_SIZE,
				executor);

		authorizationProcessor = new AuthorizationProcessor(authorization, persistence);
		eventSourceProcessor = new EventSourceProcessor(journalDirectory, this);
		persistenceProcessor = new PersistenceProcessor(persistence);
		distributionProcessor = new Distributor();

		disruptor.handleExceptionsWith(this);
		disruptor.handleEventsWith(authorizationProcessor).then(eventSourceProcessor)
				.then(persistenceProcessor).then(distributionProcessor);

		disruptor.start();
	}

	public void handleEvent(final HeliumEvent heliumEvent) {
		Preconditions.checkArgument(heliumEvent.has(HeliumEvent.TYPE),
				"No type defined in Event");
		HeliumEventTranslator eventTranslator = new HeliumEventTranslator(heliumEvent);
		logger.trace("handling event: " + heliumEvent + "(" + heliumEvent.length() + ")");
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
			snapshot.put(HeliumEvent.PAYLOAD, payload);
			snapshotJournal.get().open();
			snapshotJournal.get().write(snapshot.toString().getBytes(), WriteType.SYNC);
			snapshotJournal.get().close();
		}
	}

	public void shutdown() {
		disruptor.shutdown();
	}

	public void addEndpoint(HeliumEndpoint handler) {
		distributionProcessor.addHandler(handler);
	}

	public void removeEndpoint(HeliumEndpoint handler) {
		distributionProcessor.removeHandler(handler);
	}

	public Distributor getDistributor() {
		return distributionProcessor;
	}

	public boolean hasBacklog() {
		return currentSequence != distributionProcessor.getSequence();
	}

	@Override
	public void handleEventException(Throwable ex, long sequence, Object event) {
		logger.error("Event Exception (msg: " + ex.getMessage() + ", sequence: +" + sequence
				+ ", event: " + event + ")", ex);
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
