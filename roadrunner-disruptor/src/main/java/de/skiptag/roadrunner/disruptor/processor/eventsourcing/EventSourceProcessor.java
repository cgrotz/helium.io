package de.skiptag.roadrunner.disruptor.processor.eventsourcing;

import java.io.File;
import java.io.IOException;

import journal.io.api.ClosedJournalException;
import journal.io.api.CompactedDataFileException;
import journal.io.api.Journal;
import journal.io.api.Journal.ReadType;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;

import org.json.JSONException;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.disruptor.DisruptorRoadrunnerService;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class EventSourceProcessor implements EventHandler<RoadrunnerEvent> {

	private Journal journal = new Journal();
	private DisruptorRoadrunnerService disruptorRoadrunnerService;

	public EventSourceProcessor(File journal_dir,
			DisruptorRoadrunnerService disruptorRoadrunnerService)
			throws IOException {
		journal.setDirectory(journal_dir);
		journal.open();
		this.disruptorRoadrunnerService = disruptorRoadrunnerService;
	}

	@Override
	public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		journal.write(event.toString().getBytes(), WriteType.SYNC);
	}

	public void restore() throws ClosedJournalException,
			CompactedDataFileException, IOException, JSONException {
		for (Location location : journal.redo()) {
			byte[] record = journal.read(location, ReadType.SYNC);
			disruptorRoadrunnerService.handleEvent(new RoadrunnerEvent(new String(record)));
		}
	}

}
