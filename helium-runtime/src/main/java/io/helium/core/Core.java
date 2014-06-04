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

package io.helium.core;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import com.google.common.base.Preconditions;
import io.helium.core.processor.authorization.AuthorizationProcessor;
import io.helium.core.processor.distribution.Distributor;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import journal.io.api.ClosedJournalException;
import journal.io.api.CompactedDataFileException;
import journal.io.api.Journal;
import journal.io.api.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Core {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Core.class);
    private Persistence persistence;
    private Optional<Journal> snapshotJournal = Optional.empty();
    private long currentSequence;
    private ActorRef authorizationActor;
    private ActorSystem actorSystem;
    private ActorRef distributionActor;

    public Core(File journalDirectory, Persistence persistence, Authorization authorization) throws IOException {

        this.persistence = persistence;
        initDisruptor(journalDirectory, persistence, authorization);
    }

    public void restoreFromJournal() throws ClosedJournalException, CompactedDataFileException,
            IOException {
        Journal journal = new Journal();
        journal.setDirectory(new File("helium"));
        journal.open();
        Optional<Location> currentLocation = Optional.empty();

        Iterable<Location> redo;
        if (currentLocation.isPresent()) {
            redo = journal.redo(currentLocation.get());
        } else {
            redo = journal.redo();
        }
        for (Location location : redo) {
            byte[] record = journal.read(location, Journal.ReadType.SYNC);
            HeliumEvent heliumEvent = new HeliumEvent(new String(
                    record));
            heliumEvent.setFromHistory(true);
            Preconditions.checkArgument(heliumEvent.has(HeliumEvent.TYPE), "No type defined in Event");
            handleEvent(heliumEvent);
        }
        journal.close();
        LOGGER.info("Done restoring from journal");
    }

    @SuppressWarnings("unchecked")
    private void initDisruptor(File journalDirectory, Persistence persistence,
                               Authorization authorization) throws IOException {
        actorSystem = ActorSystem.apply("helium");
        authorizationActor = actorSystem.actorOf(new RoundRobinPool(5).props(Props.create(AuthorizationProcessor.class)), "authorizationActor");
        distributionActor = actorSystem.actorOf(new RoundRobinPool(5).props(Props.create(Distributor.class)), "distributor");

        /*
        ExecutorService executor = Executors.newCachedThreadPool();
        disruptor = new Disruptor<HeliumEvent>(Core.EVENT_FACTORY, RING_SIZE,
                executor);

        authorizationProcessor = new AuthorizationProcessor(authorization, persistence);
        eventSourceProcessor = new EventSourceProcessor(journalDirectory, this);
        persistenceProcessor = new PersistenceProcessor(persistence);
        distributionProcessor = new Distributor();

        disruptor.handleExceptionsWith(this);
        disruptor.handleEventsWith(authorizationProcessor).then(eventSourceProcessor)
                .then(persistenceProcessor).then(distributionProcessor);

        disruptor.start();*/
    }

    public void handleEvent(final HeliumEvent heliumEvent) {
        authorizationActor.tell(heliumEvent, ActorRef.noSender());
        /*Preconditions.checkArgument(heliumEvent.has(HeliumEvent.TYPE),
                "No type defined in Event");
        EventTranslator eventTranslator = new EventTranslator(heliumEvent);
        LOGGER.info("handling event: " + heliumEvent + "(" + heliumEvent.length() + ")");

        disruptor.publishEvent(eventTranslator);
        this.currentSequence = eventTranslator.getSequence();*/
    }

    public void shutdown() {
        actorSystem.shutdown();
    }

    public void handleEvent(HeliumEventType type, Optional<Node> auth, String nodePath, Optional<?> value) {
        HeliumEvent heliumEvent = new HeliumEvent(type, nodePath, value);
        if(auth.isPresent())
            heliumEvent.setAuth(auth.get());
        handle(heliumEvent);
    }

    public void handle(HeliumEvent heliumEvent) {
        handleEvent(heliumEvent);
    }

    public void distribute(String path, Node event) {
        distributionActor.tell(new Event(path, event), ActorRef.noSender());
    }

    public void distributeChangeLog(ChangeLog log) {
        distributionActor.tell(log, ActorRef.noSender());
    }
}