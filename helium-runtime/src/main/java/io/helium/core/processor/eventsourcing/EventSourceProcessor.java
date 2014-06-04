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

package io.helium.core.processor.eventsourcing;

import akka.actor.UntypedActor;
import io.helium.event.HeliumEvent;
import journal.io.api.Journal;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class EventSourceProcessor extends UntypedActor {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceProcessor.class);

    private Journal journal = new Journal();
    private Optional<Location> currentLocation = Optional.empty();

    public EventSourceProcessor(File journal_dir)
            throws IOException {
        journal.setDirectory(journal_dir);
        journal.open();
    }

    public EventSourceProcessor() throws IOException {
        journal.setDirectory(new File("helium"));
        journal.open();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        HeliumEvent event = (HeliumEvent)message;

        long startTime = System.currentTimeMillis();
        if (!event.isFromHistory() || event.isNoAuth()) {
            LOGGER.info("storing event: " + event);
            Location write = journal.write(event.toString().getBytes(), WriteType.SYNC);
            journal.sync();
            currentLocation = Optional.of(write);
        }
        LOGGER.info("onEvent "+(System.currentTimeMillis()-startTime)+"ms; event processing time "+(System.currentTimeMillis()-event.getLong("creationDate"))+"ms");
    }
}
