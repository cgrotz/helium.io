package de.skiptag.roadrunner.disruptor.processor.storage;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.dataService.DataService;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.processor.storage.actions.PushAction;
import de.skiptag.roadrunner.disruptor.processor.storage.actions.SetAction;

public class StorageProcessor implements EventHandler<RoadrunnerEvent> {

	private PushAction pushAction;

	private SetAction setAction;

	public StorageProcessor(DataService dataService) {
		pushAction = new PushAction(dataService);
		setAction = new SetAction(dataService);
	}

	@Override
	public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		try {
			switch (event.getType()) {
			case PUSH:
				pushAction.handle(event);
				break;
			case SET:
				setAction.handle(event);
				break;
			}
		} catch (Exception e) {
			throw new RuntimeException(event.toString(), e);
		}
	}
}
