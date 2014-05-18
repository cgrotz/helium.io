/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium.disruptor;

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
import journal.io.api.ClosedJournalException;
import journal.io.api.CompactedDataFileException;
import journal.io.api.Journal;
import journal.io.api.Journal.ReadType;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeliumDisruptor implements ExceptionHandler {

    public static final EventFactory<HeliumEvent> EVENT_FACTORY = new EventFactory<HeliumEvent>() {

        @Override
        public HeliumEvent newInstance() {
            return new HeliumEvent();
        }
    };
    private static final Logger logger = LoggerFactory
            .getLogger(HeliumDisruptor.class);
    private static final int RING_SIZE = 256;
    private EventSourceProcessor eventSourceProcessor;
    private PersistenceProcessor persistenceProcessor;
    private Distributor distributionProcessor;

    private Disruptor<HeliumEvent> disruptor;
    private AuthorizationProcessor authorizationProcessor;
    private Persistence persistence;
    private Optional<Journal> snapshotJournal = Optional.absent();
    private long currentSequence;

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

    public Disruptor<HeliumEvent> getDisruptor() {
        return disruptor;
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

    public void addEndpoint(HeliumEndpoint endpoint) {
        distributionProcessor.addHandler(endpoint);
    }

    public void removeEndpoint(HeliumEndpoint endpoint) {
        distributionProcessor.removeHandler(endpoint);
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
