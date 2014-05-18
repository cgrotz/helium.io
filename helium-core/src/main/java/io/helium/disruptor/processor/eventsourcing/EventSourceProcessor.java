package io.helium.disruptor.processor.eventsourcing;

import java.io.File;
import java.io.IOException;

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
import com.lmax.disruptor.EventHandler;

import io.helium.disruptor.HeliumDisruptor;
import io.helium.event.HeliumEvent;

public class EventSourceProcessor implements EventHandler<HeliumEvent> {
    private static final Logger logger = LoggerFactory.getLogger(EventSourceProcessor.class);

    private Journal journal = new Journal();
    private HeliumDisruptor helium;

    private Optional<Location> currentLocation = Optional.absent();

    private int snapshotCount = 100;

    private int messageCount = 0;

    public EventSourceProcessor(File journal_dir, HeliumDisruptor helium)
	    throws IOException {
	journal.setDirectory(journal_dir);
	journal.open();
	this.helium = helium;
    }

    @Override
    public void onEvent(HeliumEvent event, long sequence, boolean endOfBatch)
	    throws ClosedJournalException, IOException {
	if (!event.isFromHistory()) {
	    logger.trace("storing event: " + event);
	    Location write = journal.write(event.toString().getBytes(), WriteType.SYNC);
	    journal.sync();
	    currentLocation = Optional.of(write);
	    messageCount++;
	    if (messageCount > snapshotCount) {
		helium.snapshot();
		messageCount = 0;
	    }
	}
    }

    public void restore() throws ClosedJournalException,
	    CompactedDataFileException, IOException, RuntimeException {
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

    public Optional<Location> getCurrentLocation() {
	return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
	this.currentLocation = Optional.fromNullable(currentLocation);

    }

}
