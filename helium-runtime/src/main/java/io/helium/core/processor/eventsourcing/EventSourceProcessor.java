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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventHandler;
import io.helium.core.Core;
import io.helium.event.HeliumEvent;
import journal.io.api.Journal;
import journal.io.api.Journal.ReadType;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class EventSourceProcessor implements EventHandler<HeliumEvent> {
    private static final Logger logger = LoggerFactory.getLogger(EventSourceProcessor.class);

    private Journal journal = new Journal();
    private Core helium;

    private Optional<Location> currentLocation = Optional.absent();

    public EventSourceProcessor(File journal_dir, Core helium)
            throws IOException {
        journal.setDirectory(journal_dir);
        journal.open();
        this.helium = helium;
    }

    @Override
    public void onEvent(HeliumEvent event, long sequence, boolean endOfBatch)
            throws IOException {
        if (!event.isFromHistory()) {
            logger.trace("storing event: " + event);
            Location write = journal.write(event.toString().getBytes(), WriteType.SYNC);
            journal.sync();
            currentLocation = Optional.of(write);
        }
    }

    public void restore() throws IOException, RuntimeException {
        Iterable<Location> redo;
        if (currentLocation.isPresent()) {
            redo = journal.redo(currentLocation.get());
        } else {
            redo = journal.redo();
        }
        for (Location location : redo) {
            byte[] record = journal.read(location, ReadType.SYNC);
            HeliumEvent heliumEvent = new HeliumEvent(new String(
                    record));
            heliumEvent.setFromHistory(true);
            Preconditions.checkArgument(heliumEvent.has(HeliumEvent.TYPE), "No type defined in Event");
            helium.handleEvent(heliumEvent);
        }
    }
}
