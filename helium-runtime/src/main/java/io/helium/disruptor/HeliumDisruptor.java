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
import io.helium.disruptor.processor.authorization.AuthorizationProcessor;
import io.helium.disruptor.processor.distribution.Distributor;
import io.helium.disruptor.processor.eventsourcing.EventSourceProcessor;
import io.helium.disruptor.processor.persistence.PersistenceProcessor;
import io.helium.disruptor.translator.HeliumEventTranslator;
import io.helium.event.HeliumEvent;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.web.messaging.HeliumEndpoint;
import journal.io.api.ClosedJournalException;
import journal.io.api.CompactedDataFileException;
import journal.io.api.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

    public HeliumDisruptor(File journalDirectory, Persistence persistence, Authorization authorization) throws IOException {

        this.persistence = persistence;
        initDisruptor(journalDirectory, persistence, authorization);
        restoreFromJournal();
    }

    public Disruptor<HeliumEvent> getDisruptor() {
        return disruptor;
    }

    private void restoreFromJournal() throws ClosedJournalException, CompactedDataFileException,
            IOException {
        eventSourceProcessor.restore();
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
